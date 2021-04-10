package com.fileserver.app.controller;

import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;

import com.fileserver.app.entity.file.FileModel;
import com.fileserver.app.exception.NotFoundException;
import com.fileserver.app.exception.NotSupportedException;
import com.fileserver.app.handler.VideoHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/vs")
public class VideoControllerSpec {

    private VideoHandler handler;

    String completed = "completed";
    String uploaded = "uploaded";
    String bucket = "nomore";

    @Autowired
    public VideoControllerSpec(VideoHandler handler) {
        this.handler = handler;
    }

    private String fnf = "file not found";
    private String type = "video";
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

    @PostMapping("/process/{id}/preview")
    public CompletableFuture<FileModel> process(@PathVariable("id") String id, @RequestParam("key") String name,
            @RequestParam("contentType") String contentType) {
        return CompletableFuture.supplyAsync(() -> handler.preview(id, name, contentType));
    }

    @PostMapping("/process/{id}/resolution")
    public CompletableFuture<FileModel> processRe(@PathVariable("id") String id, @RequestParam("key") String name,
            @RequestParam("contentType") String contentType) {
        return CompletableFuture.supplyAsync(() -> handler.resolution(id, name, contentType));
    }

    @PostMapping("/process/{id}/{sub}")
    public CompletableFuture<FileModel> uploadSub(@PathVariable("id") String id, @RequestParam("key") String name,
            @RequestParam("contentType") String contentType) {
        return CompletableFuture.supplyAsync(() -> handler.preview(id, name, contentType));
    }

}
