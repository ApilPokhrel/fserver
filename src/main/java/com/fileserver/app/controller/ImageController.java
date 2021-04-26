package com.fileserver.app.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;

import com.fileserver.app.entity.file.FileModel;
import com.fileserver.app.entity.file.Resolution;
import com.fileserver.app.exception.NotFoundException;
import com.fileserver.app.exception.NotSupportedException;
import com.fileserver.app.handler.ImageHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/image")
public class ImageController {

    private ImageHandler handler;

    String completed = "completed";
    String uploaded = "uploaded";
    String bucket = "nomore";

    @Autowired
    public ImageController(ImageHandler handler) {
        this.handler = handler;
    }

    private String fnf = "file not found";
    private String type = "image";
    private String fnv = "File is not type of video";
    private String originHeader = "origin";

    @PostMapping("/save")
    public CompletableFuture<FileModel> save(@RequestParam("file") MultipartFile file, @RequestParam("key") String name,
            @RequestParam("contentType") String contentType, @RequestParam("uuid") String uuid,
            HttpServletRequest req) {
        if (file == null)
            throw new NotFoundException(fnf);

        if (contentType == null || !contentType.contains(type))
            throw new NotSupportedException(fnv);

        if (name.isBlank())
            throw new NotFoundException("name not found");

        if (uuid.isBlank())
            throw new NotFoundException("uuid not found");

        return CompletableFuture.supplyAsync(() -> handler.save(file, req.getHeader(originHeader), name, type, uuid));
    }

    @PostMapping("/upload/{id}")
    public CompletableFuture<FileModel> upload(@PathVariable("id") String id) {
        return CompletableFuture.supplyAsync(() -> handler.upload(id));
    }

    @PostMapping("/complete/{id}")
    public CompletableFuture<FileModel> complete(@PathVariable("id") String id) {
        return CompletableFuture.supplyAsync(() -> handler.complete(id));
    }

    @PostMapping("/resize/{id}")
    public CompletableFuture<List<FileModel>> resize(@PathVariable("id") String id,
            @RequestBody List<Resolution> resolutions) {
        return CompletableFuture.supplyAsync(
                () -> handler.resize(id, resolutions == null ? Resolution.getImageFormatList() : resolutions));
    }

}
