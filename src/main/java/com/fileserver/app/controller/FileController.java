package com.fileserver.app.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.fileserver.app.dao.file.FileInterface;
import com.fileserver.app.dao.user.UserRolesInterface;
import com.fileserver.app.entity.file.FileBody;
import com.fileserver.app.entity.file.FileModel;
import com.fileserver.app.entity.user.User;
import com.fileserver.app.exception.NotFoundException;
import com.fileserver.app.exception.NotSupportedException;
import com.fileserver.app.service.AWSUploadService;
import com.fileserver.app.service.AuthService;
import com.fileserver.app.service.FileService;
import com.fileserver.app.util.TokenUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private AWSUploadService awsUploadService;
    private FileInterface fileInterface;
    private AuthService authService;
    private UserRolesInterface rolesInterface;

    String completed = "completed";
    String uploaded = "uploaded";
    String bucket = "nomore";

    private String fnf = "file not found";
    private String originHeader = "origin";
    private String fns = "file not saved in db";

    @Autowired
    public FileController(FileService fileService, AWSUploadService awsUploadService, FileInterface fileInterface,
            AuthService authService, UserRolesInterface rolesInterface) {
        this.fileService = fileService;
        this.awsUploadService = awsUploadService;
        this.fileInterface = fileInterface;
        this.authService = authService;
        this.rolesInterface = rolesInterface;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody FileBody body) {
        User user = authService.login(body.getUsername(), body.getPassword());
        Optional<FileModel> filePayload = fileInterface.getByName(body.getName());
        Map<String, Object> res = new HashMap<>();
        if (filePayload.isPresent() && body.isReplace()) {
            res.put("url", "/file/replace" + filePayload.get().get_id());
            res.put("file", filePayload.get());
        } else if (filePayload.isPresent() && body.isRemove()) {
            res.put("url", "/file/remove" + filePayload.get().get_id());
            res.put("file", filePayload.get());
        } else {
            res.put("url", "/file/upload");
        }
        res.put("user", user);
        res.put("token", TokenUtil.create(user.get_id(), body.getExp(), user.queryPerms(rolesInterface)));
        return res;
    }

    @PostMapping("/upload")
    public CompletableFuture<FileModel> upload(@RequestParam("file") MultipartFile file,
            @RequestParam("key") String name, @RequestParam("contentType") String contentType,
            @RequestParam("uuid") String uuid, HttpServletRequest request) {
        if (name.isBlank())
            throw new NotFoundException("name not found");

        if (uuid.isBlank())
            throw new NotFoundException("uuid not found");
        return CompletableFuture.supplyAsync(() -> {
            FileModel model = save(file, request.getHeader(originHeader), name, contentType, uuid);
            upload(model.getName(), model.getMimeType());
            remove(model.getName());
            return model;
        });
    }

    @DeleteMapping("/remove/{id}")
    public CompletableFuture<FileModel> removeFile(@PathVariable("id") String id) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<FileModel> isFile = fileInterface.removeById(id);
            if (!isFile.isPresent())
                throw new NotFoundException("file not found");

            if (!isFile.get().isCompleted()) {
                fileService.remove(isFile.get().getName());
            }
            awsUploadService.remove(bucket, isFile.get().getName());
            return isFile.get();
        });
    }

    @PostMapping("/replace")
    public CompletableFuture<FileModel> upload(@PathVariable("id") String id, @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Optional<FileModel> isFile = fileInterface.removeById(id);
        if (!isFile.isPresent())
            throw new NotFoundException("cannot replace file not found");

        if (!isFile.get().isCompleted()) {
            fileService.remove(isFile.get().getName());
        }

        awsUploadService.remove(bucket, isFile.get().getName());
        return CompletableFuture.supplyAsync(() -> {
            FileModel model = save(file, request.getHeader(originHeader), isFile.get().getName(),
                    isFile.get().getMimeType(), isFile.get().getUuid());
            upload(model.getName(), model.getMimeType());
            remove(model.getName());
            return model;
        });
    }

    private FileModel save(MultipartFile file, String origin, String name, String contentType, String uuid) {
        if (file == null) {
            throw new NotFoundException(fnf);
        }
        if (contentType == null) {
            throw new NotSupportedException("content type not supported");
        }

        FileModel fileModel = new FileModel();
        fileService.uploadFile(file, name); // after this
        fileModel.setName(name);
        fileModel.setType(contentType.split("/")[0]);
        fileModel.setMimeType(contentType);
        fileModel.setUuid(uuid);
        fileModel.setSize(file.getSize());
        fileModel.setOrigin(origin);
        return fileInterface.add(fileModel).orElseThrow(() -> new NotFoundException(fns));
    }

    private FileModel upload(String name, String contentType) {
        awsUploadService.upload(bucket, name, contentType);
        return fileInterface.updateStatus(name, true, true, false)
                .orElseThrow(() -> new NotFoundException("file not updated while upload"));
    }

    private FileModel remove(String name) {
        boolean isCompleted = fileService.remove(name);
        return fileInterface.updateByName(name, completed, isCompleted)
                .orElseThrow(() -> new NotSupportedException("file not removed"));
    }

}
