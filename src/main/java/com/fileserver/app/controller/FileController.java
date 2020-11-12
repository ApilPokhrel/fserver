package com.fileserver.app.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.fileserver.app.dao.file.FileInterface;
import com.fileserver.app.dao.user.UserRolesInterface;
import com.fileserver.app.entity.file.File;
import com.fileserver.app.entity.file.FileBody;
import com.fileserver.app.entity.user.User;
import com.fileserver.app.exception.NotFoundException;
import com.fileserver.app.exception.NotSupportedException;
import com.fileserver.app.service.AWSUploadService;
import com.fileserver.app.service.AuthService;
import com.fileserver.app.service.FFMPEGService;
import com.fileserver.app.service.FileService;
import com.fileserver.app.util.TokenUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/file")
public class FileController {

    private FileService fileService;
    private FFMPEGService ffmpegService;
    private AWSUploadService awsUploadService;
    private FileInterface fileInterface;
    private AuthService authService;
    private UserRolesInterface rolesInterface;

    String completed = "completed";
    String uploaded = "uploaded";
    String bucket = "nomore";

    @Autowired
    public FileController(FileService fileService, FFMPEGService ffmpegService, AWSUploadService awsUploadService,
            FileInterface fileInterface, AuthService authService, UserRolesInterface rolesInterface) {
        this.fileService = fileService;
        this.ffmpegService = ffmpegService;
        this.awsUploadService = awsUploadService;
        this.fileInterface = fileInterface;
        this.authService = authService;
        this.rolesInterface = rolesInterface;
    }

    private String fnf = "file not found";
    private String videoType = "video/";
    private String fnv = "File is not type of video";
    private String originHeader = "origin";

    @PostMapping("/upload/video/async")
    public CompletableFuture<Object> uploadFileAsync(@RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        if (file == null) {
            throw new NotFoundException(fnf);
        }
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();
        if (contentType != null && !contentType.startsWith(videoType)) {
            throw new NotSupportedException(fnv);
        }

        // File upload first
        // Parallel -> Upload to AWS, Create a preview
        // After preview -> upload preview to aws

        File fileModel = new File();

        Optional<File> isFile = fileInterface.getByName(name);
        if (!isFile.isPresent()) {
            fileService.uploadFile(file); // after this
            String origin = request.getHeader(originHeader);
            fileModel.setName(name);
            fileModel.setType(contentType);
            fileModel.setSize(file.getSize());
            fileModel.setOrigin(origin);
            fileModel = fileInterface.add(fileModel).orElseThrow(() -> new NotFoundException("file not saved in db"));
        } else {
            fileModel = isFile.get();
            if (!fileService.exists(fileModel.getName())) {
                fileService.uploadFile(file); // if not in directory upload file
                fileInterface.updateByName(fileModel.getName(), uploaded, false);
                fileInterface.updateByName(fileModel.getName(), completed, false);
            }
        }
        final boolean is_uploaded = fileModel.isUploaded();
        return CompletableFuture.supplyAsync(() -> {
            if (!is_uploaded) {
                return awsUploadService.multipartUploadAsync(bucket, name, contentType).thenApplyAsync(f -> {
                    Optional<File> fm = fileInterface.updateByName(name, uploaded, true);
                    preview(fm.get());
                    return fm.get();
                });

            } else {
                return fileInterface.getByName(name).get();
            }
        });
    }

    // replace or not checking route should be different
    // check (name, replace) -> token, url, file
    // file replace or not
    // replace new route that only uploads
    // and another as same

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody FileBody body) {
        User user = authService.login(body.getUsername(), body.getPassword());
        Optional<File> filePayload = fileInterface.getByName(body.getName());
        if (filePayload.isPresent()) {
            if (body.isReplace()) {
                // send a route that will delete file from DB and upload it
                // No checks like first time
                Map<String, Object> res = new HashMap<>();
                res.put("user", user);
                res.put("url", "/upload/video/replace/"+filePayload.get().get_id());
                res.put("token", TokenUtil.create(user.get_id(), body.getExp(), user.queryPerms(rolesInterface)));
                return res;
            } else {
                throw new NotSupportedException("Name already taken");
            }
        } else {
            // Check File in drive if not upload
            // Check Uploaded if not upload
            // Check Completed if not complete
            Map<String, Object> res = new HashMap<>();
            res.put("user", user);
            res.put("url", "/upload/video");
            res.put("token", TokenUtil.create(user.get_id(), body.getExp(), user.queryPerms(rolesInterface)));
            return res;
        }
    }

    @PostMapping("/upload/video")
    public CompletableFuture<File> uploadFileSync(@RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        if (file == null) {
            throw new NotFoundException("file not found");
        }
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();
        if (contentType != null && !contentType.startsWith("video/")) {
            throw new NotSupportedException("File is not type of video");
        }

        File fileModel = new File();

        Optional<File> isFile = fileInterface.getByName(name);
        if (!isFile.isPresent()) {
            fileService.uploadFile(file); // after this
            String origin = request.getHeader("origin");
            fileModel.setName(name);
            fileModel.setType(contentType);
            fileModel.setSize(file.getSize());
            fileModel.setOrigin(origin);
            fileModel = fileInterface.add(fileModel).orElseThrow(() -> new NotFoundException("file not saved in db"));
        } else {
            fileModel = isFile.get();
            if (!fileService.exists(fileModel.getName())) {
                fileService.uploadFile(file); // if not in directory upload file
                fileInterface.updateByName(fileModel.getName(), uploaded, false);
                fileInterface.updateByName(fileModel.getName(), completed, false);
            }
        }
        final boolean is_uploaded = fileModel.isUploaded();
        return CompletableFuture.supplyAsync(() -> {
            if (!is_uploaded) {
                awsUploadService.multipartUploadSync(bucket, name, contentType);
                Optional<File> fm = fileInterface.updateByName(name, uploaded, true);
                preview(fm.get());
                return fm.get();
            } else {
                return fileInterface.getByName(name).get();
            }
        });
    }

    @PostMapping("/upload/video/replace/{id}")
    public CompletableFuture<File> replaceFileSync(@RequestParam("file") MultipartFile file,
            @PathVariable("id") String id, HttpServletRequest request) {
        if (file == null) {
            throw new NotFoundException(fnf);
        }
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();
        if (contentType != null && !contentType.startsWith(videoType)) {
            throw new NotSupportedException(fnv);
        }

        Optional<File> isFile = fileInterface.removeById(id);
        if (!isFile.isPresent())
            throw new NotSupportedException("cannot replace file not found");
        File fileModel = isFile.get();

        fileService.remove(fileModel.getName());
        CompletableFuture.runAsync(() -> awsUploadService.remove(bucket, fileModel.getName()));

        String origin = request.getHeader(originHeader);
        fileModel.setName(name);
        fileModel.setType(contentType);
        fileModel.setSize(file.getSize());
        fileModel.setOrigin(origin);
        fileInterface.add(fileModel); //save file

        return CompletableFuture.supplyAsync(() -> {
            awsUploadService.multipartUploadSync(bucket, name, contentType);
            Optional<File> fm = fileInterface.updateByName(name, uploaded, true);
            preview(fm.get());
            return fm.get();
        });
    }

    @PostMapping("/clip-test")
    public boolean test() {
        ffmpegService.createPreview("72.mp4");
        return true;
    }

    @PostMapping("/thumnail-test")
    public boolean thumbnailTest() {
        ffmpegService.generateImage("72.mp4", "thumbnail-test-2", 120d);
        return true;
    }

    private File preview(File d) {
        Optional<File> isPreview = fileInterface.incompleted(d.getName());
        File previewModel = new File();
        if (!isPreview.isPresent()) {
            String preview = ffmpegService.createPreview(d.getName()); // db store preview created
            previewModel.setName(preview);
            previewModel.setType(d.getType());
            previewModel.setParent(d.getName());
            previewModel.set_parent(false);
            previewModel = fileInterface.add(previewModel)
                    .orElseThrow(() -> new NotFoundException("sub-file not saved in db"));
        } else {
            previewModel = isPreview.get();
            if (!fileService.exists(previewModel.getName())) {
                ffmpegService.createPreview(d.getName()); // if not in directory upload file
                fileInterface.updateByName(previewModel.getName(), uploaded, false);
                fileInterface.updateByName(previewModel.getName(), completed, false);
            }
        }

        if (!previewModel.isUploaded()) {
            awsUploadService.upload("nomore", previewModel.getName(), "video/mp4"); // db store preview uploaded
            fileInterface.updateByName(previewModel.getName(), uploaded, true);
            boolean isCompleted = fileService.remove(previewModel.getName());
            fileInterface.updateByName(previewModel.getName(), completed, isCompleted);
        } else if (!previewModel.isCompleted()) {
            boolean isCompleted = fileService.remove(previewModel.getName());
            fileInterface.updateByName(previewModel.getName(), completed, isCompleted);
        }

        if (!d.isCompleted()) {
            boolean isCompleted = fileService.remove(d.getName());
            fileInterface.updateByName(d.getName(), completed, isCompleted);
        }
        return d;
    }
}
