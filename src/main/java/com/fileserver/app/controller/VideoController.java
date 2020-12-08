package com.fileserver.app.controller;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.fileserver.app.dao.file.FileInterface;
import com.fileserver.app.dao.user.UserRolesInterface;
import com.fileserver.app.entity.file.FileModel;
import com.fileserver.app.entity.file.ResponseBody;
import com.fileserver.app.entity.file.VideoBody;
import com.fileserver.app.entity.user.User;
import com.fileserver.app.exception.NotFoundException;
import com.fileserver.app.exception.NotSupportedException;
import com.fileserver.app.handler.VideoHandler;
import com.fileserver.app.service.AWSUploadService;
import com.fileserver.app.service.AuthService;
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
@RequestMapping("/api/v1/video")
public class VideoController {

    private FileService fileService;
    private FileInterface fileInterface;
    private AuthService authService;
    private UserRolesInterface rolesInterface;
    private VideoHandler handler;
    private AWSUploadService awsUploadService;

    String completed = "completed";
    String uploaded = "uploaded";
    String bucket = "nomore";

    @Autowired
    public VideoController(FileService fileService, FileInterface fileInterface, AuthService authService,
            UserRolesInterface rolesInterface, VideoHandler handler, AWSUploadService awsUploadService) {
        this.fileService = fileService;
        this.fileInterface = fileInterface;
        this.authService = authService;
        this.rolesInterface = rolesInterface;
        this.handler = handler;
        this.awsUploadService = awsUploadService;
    }

    private String fnf = "file not found";
    private String type = "video";
    private String fnv = "File is not type of video";
    private String originHeader = "origin";

    @PostMapping("/login")
    public ResponseBody login(@Valid @RequestBody VideoBody body) {
        User user = authService.login(body.getUsername(), body.getPassword());
        StringBuilder sb = new StringBuilder();
        ResponseBody res = new ResponseBody();
        Optional<FileModel> fOptional = fileInterface.getByName(body.getName());

        if (fOptional.isPresent()) {
            FileModel model = fOptional.get();
            if (!model.getUuid().equals(body.getUuid()))
                throw new NotSupportedException("file name already taken");

            boolean exists = fileService.exists(body.getName());
            if (body.isPreview() && exists) {
                sb.append("/video/preview/" + model.get_id() + "/exists");
                res.setMethod("post");
                res.setMultipart(false);
            } else if (body.isPreview() && !exists) {
                sb.append("/video/preview/" + model.get_id() + "/notexists");
                res.setMethod("post");
                res.setMultipart(false);
            } else if (body.isReplace() || !exists) {
                sb.append("/video/replace/" + model.get_id());
                res.setMethod("post");
                res.setMultipart(true);
            } else if (!model.isUploaded()) {
                sb.append("/video/process/" + model.get_id());
                res.setMethod("post");
                res.setMultipart(false);
            } else if (!model.isCompleted() && model.isUploaded()) {
                sb.append("/video/complete/" + model.get_id());
                res.setMethod("post");
                res.setMultipart(false);
            }

        } else if (body.isPreview()) {
            sb.append("/video/preview/");
            sb.append("?key=" + body.getName());
            sb.append("&contentType=" + body.getContentType());
            res.setMethod("post");
            res.setMultipart(false);
        } else {
            sb.append("/video/upload");
            sb.append("?key=" + body.getName());
            sb.append("&contentType=" + body.getContentType());
            res.setMethod("post");
            res.setMultipart(true);
        }

        res.setUrl(sb.toString());
        res.setUser(user);
        res.setToken(TokenUtil.create(user.get_id(), body.getExp(), user.queryPerms(rolesInterface)));
        return res;
    }

    @PostMapping("/upload")
    public CompletableFuture<FileModel> uploadFileSync(@RequestParam("file") MultipartFile file,
            @RequestParam("key") String name, @RequestParam("contentType") String contentType,
            HttpServletRequest request) {
        if (file == null) {
            throw new NotFoundException(fnf);
        }
        if (contentType == null || !contentType.contains(type)) {
            throw new NotSupportedException(fnv);
        }

        String id = handler.save(file, request.getHeader(originHeader)).get_id();

        CompletableFuture<FileModel> toAws = CompletableFuture.supplyAsync(() -> handler.upload(name, contentType));

        CompletableFuture<FileModel> toPreview = CompletableFuture
                .supplyAsync(() -> handler.preview(id, name, contentType));

        return toAws.thenCombine(toPreview, (aws, prev) -> handler.complete(name));
    }

    @PostMapping("/process/{id}")
    public CompletableFuture<FileModel> process(@PathVariable("id") String id) {
        FileModel model = fileInterface.getById(id).orElseThrow();
        CompletableFuture<FileModel> toAws = CompletableFuture
                .supplyAsync(() -> handler.upload(model.getName(), model.getMimeType()));

        CompletableFuture<FileModel> toPreview = CompletableFuture
                .supplyAsync(() -> handler.preview(model.get_id(), model.getName(), model.getMimeType()));

        return toAws.thenCombine(toPreview, (aws, prev) -> handler.complete(model.getName()));
    }

    @PostMapping("/complete/{id}")
    public CompletableFuture<FileModel> upload(@PathVariable("id") String id) {
        FileModel model = fileInterface.getById(id).orElseThrow();
        return CompletableFuture.supplyAsync(() -> handler.complete(model.getName()));
    }

    @PostMapping("/replace/{id}")
    public CompletableFuture<FileModel> replaceFileSync(@RequestParam("file") MultipartFile file,
            @PathVariable("id") String id, HttpServletRequest request) {
        // first check file and it's sub files recursively
        if (file == null) {
            throw new NotFoundException(fnf);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.contains(type)) {
            throw new NotSupportedException(fnv);
        }

        Optional<FileModel> isFile = fileInterface.removeById(id);
        if (!isFile.isPresent())
            throw new NotSupportedException("cannot replace file not found");

        FileModel fileModel = isFile.get();
        String name = fileModel.getName();

        handler.remove(name, contentType);
        String origin = request.getHeader(originHeader);
        String parentId = handler.save(file, origin).get_id();
        List<FileModel> subFiles = fileInterface.listSubFile(parentId);
        subFiles.forEach(f -> handler.removePreview(f.getName(), f.getMimeType()));
        CompletableFuture<FileModel> toAws = CompletableFuture.supplyAsync(() -> handler.upload(name, contentType));

        CompletableFuture<FileModel> toPreview = CompletableFuture
                .supplyAsync(() -> handler.preview(parentId, name, contentType));

        return toAws.thenCombine(toPreview, (aws, prev) -> handler.complete(name));
    }

    @PostMapping("/preview")
    public CompletableFuture<FileModel> previewUpload(@RequestParam("key") String name,
            @RequestParam("contentType") String contentType) {
        return CompletableFuture.supplyAsync(() -> {
            File file = awsUploadService.downloadFile(bucket, name);
            boolean ex = file != null && file.exists() && file.canRead();
            if (!ex)
                throw new NotFoundException("file not found in cloud");

            FileModel fileModel = new FileModel();
            fileModel.setName(name);
            fileModel.setType(contentType);
            fileModel.setSize(file.length());
            fileModel.setOrigin("cloud");
            String parentId = fileInterface.add(fileModel).orElseThrow().get_id();

            FileModel prev = handler.preview(parentId, name, contentType);
            handler.complete(name);
            return prev;
        });
    }

    @PostMapping("/preview/{id}/exists")
    public CompletableFuture<FileModel> previewExists(@PathVariable("id") String parentId) {
        return CompletableFuture.supplyAsync(() -> {
            FileModel model = fileInterface.getById(parentId).orElseThrow();
            FileModel prev = handler.preview(model.get_id(), model.getName(), model.getMimeType());
            handler.complete(model.getName());
            return prev;
        });
    }

    @PostMapping("/preview/{id}/notexists")
    public CompletableFuture<FileModel> previewNotExists(@PathVariable("id") String parentId) {
        return CompletableFuture.supplyAsync(() -> {
            FileModel model = fileInterface.getById(parentId).orElseThrow();
            File file = awsUploadService.downloadFile(bucket, model.getName());
            boolean ex = file != null && file.exists() && file.canRead();
            if (!ex)
                throw new NotFoundException("file not found in cloud");
            FileModel prev = handler.preview(model.get_id(), model.getName(), model.getMimeType());
            handler.complete(model.getName());
            return prev;
        });
    }
}
