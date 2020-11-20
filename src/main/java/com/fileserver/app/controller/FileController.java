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
    private String videoType = "video/mp4";
    private String type = "video/";
    private String fnv = "File is not type of video";
    private String originHeader = "origin";
    private String fns = "file not saved in db";

    // replace or not checking route should be different
    // check (name, replace) -> token, url, file
    // file replace or not
    // replace new route that only uploads
    // and another as same

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody FileBody body) {
        User user = authService.login(body.getUsername(), body.getPassword());
        Optional<File> filePayload = fileInterface.getByName(body.getName());
        Map<String, Object> res = new HashMap<>();
        if (filePayload.isPresent() && body.isReplace()) {
            res.put("url", "/upload/video/replace/" + filePayload.get().get_id());
            res.put("file", filePayload.get());
        } else if(filePayload.isPresent() && body.isPreview()){
            res.put("url", "/upload/video/preview");
            res.put("file", filePayload.get());
        }  else {
            res.put("url", "/upload/video");
        }
        res.put("user", user);
        res.put("token", TokenUtil.create(user.get_id(), body.getExp(), user.queryPerms(rolesInterface)));
        return res;

    }

    @PostMapping("/upload/video")
    public CompletableFuture<File> uploadFileSync(@RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        if (file == null) {
            throw new NotFoundException(fnf);
        }
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();
        if (contentType == null || !contentType.startsWith(type)) {
            throw new NotSupportedException(fnv);
        }

        File fileModel;

        Optional<File> isFile = fileInterface.getByName(name);
        if (!isFile.isPresent()) { // not present
            fileModel = saveVideo(file, request.getHeader(originHeader));
        } else { // present
            fileModel = isFile.get();
            if (!fileService.exists(fileModel.getName())) { // exists in storage or not
                fileService.uploadFile(file); // if not in directory save in storage
                fileModel = fileInterface.updateStatus(fileModel.getName(), false, false).orElseThrow();
            }
        }

        final boolean is_uploaded = fileModel.isUploaded();
        CompletableFuture<File> toAws = CompletableFuture.supplyAsync(() -> {
            if (!is_uploaded)
                return upload(name, contentType);
            else
                return fileInterface.getByName(name).orElseThrow();
        });

        CompletableFuture<File> toPreview = CompletableFuture.supplyAsync(() -> preview(name, contentType));

        return toAws.thenCombine(toPreview, (aws, prev) -> removeFile(name));
    }

    @PostMapping("/upload/video/replace/{id}")
    public CompletableFuture<File> replaceFileSync(@RequestParam("file") MultipartFile file,
            @PathVariable("id") String id, HttpServletRequest request) {
        if (file == null) {
            throw new NotFoundException(fnf);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith(type)) {
            throw new NotSupportedException(fnv);
        }

        Optional<File> isFile = fileInterface.removeById(id);
        if (!isFile.isPresent())
            throw new NotSupportedException("cannot replace file not found");

        File fileModel = isFile.get();
        String name = fileModel.getName();

        removeFile(name, contentType);

        fileService.uploadFile(file);

        String origin = request.getHeader(originHeader);
        fileModel = saveVideo(file, origin);

        final boolean is_uploaded = fileModel.isUploaded();
        CompletableFuture<File> toAws = CompletableFuture.supplyAsync(() -> {
            if (!is_uploaded)
                return upload(name, contentType);
            else
                return fileInterface.getByName(name).orElseThrow();
        });

        CompletableFuture<File> toPreview = CompletableFuture.supplyAsync(() -> preview(name, contentType));

        return toAws.thenCombine(toPreview, (aws, prev) -> removeFile(name));
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

    private File preview(String filename, String type) {// if video file is pr
        Optional<File> isPreview = fileInterface.incompleted(filename);
        File previewModel;
        if (isPreview.isPresent() && fileService.exists(isPreview.get().getName())) {
            previewModel = isPreview.get();
            if (!previewModel.isUploaded()) {
                uploadPreview(previewModel.getName());
            }
        } else {
            if (isPreview.isPresent())
                fileInterface.removeById(isPreview.get().get_id());
            previewModel = savePreview(filename, type);
            uploadPreview(previewModel.getName());
        }
        return removePreview(previewModel.getName());
    }

    private void removeFile(String name, String contentType) {
        fileService.remove(name);
        awsUploadService.remove(bucket, name);
        Optional<File> preview = fileInterface.removeChild(name, contentType);
        if (preview.isPresent()) {
            fileService.remove(preview.get().getName());
            awsUploadService.remove(bucket, preview.get().getName());
        }
    }

    // Chunk the upload
    // login -> check name , replace
    // -> created, uploaded, processed, completed
    // -> created, uploaded, processed, completed (is_parent = false)

    // 1.created
    // -> save file to storage, create file in db
    // -> upload to aws, update uploaded to true db
    // -> generate preview, create preview file in db with parent
    // -> upload preview to aws, update preview uploaded to true
    // -> delete preview from storage, update preview completed to true in db
    // -> delete file from storage, update completed to true in db

    private File saveVideo(MultipartFile file, String origin) {
        if (file == null) {
            throw new NotFoundException(fnf);
        }
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();
        if (contentType == null || !contentType.startsWith(type)) {
            throw new NotSupportedException(fnv);
        }

        File fileModel = new File();
        fileService.uploadFile(file); // after this
        fileModel.setName(name);
        fileModel.setType(contentType);
        fileModel.setSize(file.getSize());
        fileModel.setOrigin(origin);
        return fileInterface.add(fileModel).orElseThrow(() -> new NotFoundException(fns));
    }

    @PostMapping("/upload/video/preview")
    public File previewUpload(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String contentType = body.get("type");
        if(!fileService.exists(name)){
            boolean ex = awsUploadService.downloadFile(bucket, name);
            if(!ex) throw new NotFoundException("file not found in cloud");
        }
        File prev = preview(name, contentType);
        removeFile(name);
        return prev;
    }

    private File upload(String name, String contentType) {
        awsUploadService.multipartUploadSync(bucket, name, contentType);
        return fileInterface.updateStatus(name, true, false)
                .orElseThrow(() -> new NotFoundException("file not updated while upload"));
    }

    private File savePreview(String fileName, String fileType) {
        File previewModel = new File();
        String preview = ffmpegService.createPreview(fileName); // db store preview created
        previewModel.setName(preview);
        previewModel.setType(fileType);
        previewModel.setParent(fileName);
        previewModel.set_parent(false);
        return fileInterface.add(previewModel).orElseThrow(() -> new NotFoundException("preview not saved in db"));
    }

    private File uploadPreview(String preview) {
        awsUploadService.upload(bucket, preview, videoType); // db store preview uploaded
        return fileInterface.updateStatus(preview, true, false)
                .orElseThrow(() -> new NotFoundException("preview not updated while uploading"));
    }

    private File removePreview(String preview) {
        boolean isCompleted = fileService.remove(preview);
        return fileInterface.updateByName(preview, completed, isCompleted)
                .orElseThrow(() -> new NotSupportedException("preview not removed"));
    }

    private File removeFile(String name) {
        boolean isCompleted = fileService.remove(name);
        return fileInterface.updateByName(name, completed, isCompleted)
                .orElseThrow(() -> new NotSupportedException("file not removed"));
    }

}
