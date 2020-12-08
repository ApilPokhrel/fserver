package com.fileserver.app.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.fileserver.app.dao.file.FileInterface;
import com.fileserver.app.dao.user.UserRolesInterface;
import com.fileserver.app.entity.file.FileModel;
import com.fileserver.app.entity.file.ResponseBody;
import com.fileserver.app.entity.file.FileBody;
import com.fileserver.app.entity.user.User;
import com.fileserver.app.exception.NotFoundException;
import com.fileserver.app.exception.NotSupportedException;
import com.fileserver.app.service.AWSUploadService;
import com.fileserver.app.service.AuthService;
import com.fileserver.app.service.FFMPEGService;
import com.fileserver.app.service.FileService;
import com.fileserver.app.service.PDFService;
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
@RequestMapping("/api/v1/pdf")
public class PDFController {

    private FileService fileService;
    private AWSUploadService awsUploadService;
    private FileInterface fileInterface;
    private AuthService authService;
    private UserRolesInterface rolesInterface;
    private PDFService pdfServices;

    String completed = "completed";
    String uploaded = "uploaded";
    String bucket = "nomore";
    String imageType = "image/jpeg";

    private String fnf = "file not found";
    private String type = "pdf";
    private String fnv = "File is not type of video";
    private String originHeader = "origin";
    private String fns = "file not saved in db";

    @Autowired
    public PDFController(FileService fileService, FFMPEGService ffmpegService, AWSUploadService awsUploadService,
            FileInterface fileInterface, AuthService authService, UserRolesInterface rolesInterface,
            PDFService pdfServices) {
        this.fileService = fileService;
        this.awsUploadService = awsUploadService;
        this.fileInterface = fileInterface;
        this.authService = authService;
        this.rolesInterface = rolesInterface;
        this.pdfServices = pdfServices;
    }

    @PostMapping("/login")
    public ResponseBody login(@Valid @RequestBody FileBody body) {
        User user = authService.login(body.getUsername(), body.getPassword());
        Optional<FileModel> filePayload = fileInterface.getByName(body.getName());
        ResponseBody result = new ResponseBody();
        if (filePayload.isPresent()) {
            FileModel model = filePayload.get();
            if (!fileService.exists(model.getName()) || body.isReplace()) {
                result.setFile(filePayload.get());
                result.setUrl("/pdf/replace" + filePayload.get().get_id());
                result.setMethod("post");
                result.setMultipart(true);
            } else {
                if (!model.isUploaded()) {
                    result.setUrl("/pdf/process" + model.get_id());
                } else if (!model.isCompleted()) {
                    result.setUrl("/pdf/complete" + model.get_id());
                }
                result.setFile(model);
                result.setMethod("post");
            }
        } else {
            result.setUrl("/pdf/upload");
            result.setMultipart(true);
            result.setMethod("post");
        }
        result.setUser(user);
        result.setToken(TokenUtil.create(user.get_id(), body.getExp(), user.queryPerms(rolesInterface)));
        return result;
    }

    @PostMapping("/pdf-to-images")
    public CompletableFuture<List<FileModel>> pdfToImages() {
        return CompletableFuture.supplyAsync(() -> {
            List<FileModel> list = new ArrayList<>();
            File file = new File("/home/mr/Documents/test.pdf");
            pdfServices.generateImages("3234324ewqwe", file, f -> {
                FileModel fm = fileInterface.add(f).orElseThrow();
                list.add(fm);
            });
            return list;
        });
    }

    @PostMapping("/upload")
    public CompletableFuture<List<FileModel>> upload(@RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        FileModel model = save(file, request.getHeader(originHeader));
        return CompletableFuture.supplyAsync(() -> {
            List<FileModel> list = new ArrayList<>();
            File fle = fileService.get(model.getName());
            pdfServices.generateImages(model.get_id(), fle, f -> {
                awsUploadService.upload(bucket, f.getName(), imageType);
                FileModel fm = fileInterface.add(f).orElseThrow();
                list.add(fm);
            });
            upload(model.getName(), model.getMimeType());
            boolean comp = pdfServices.emptyImages();
            if (comp)
                remove(model.getName());
            return list;
        });
    }

    @PostMapping("/save")
    public CompletableFuture<FileModel> save(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() -> save(file, request.getHeader(originHeader)));
    }

    @PostMapping("/process/{id}")
    public CompletableFuture<List<FileModel>> process(@PathVariable("id") String id) {
        FileModel model = fileInterface.getById(id).orElseThrow();
        return CompletableFuture.supplyAsync(() -> {
            List<FileModel> list = new ArrayList<>();
            File fle = fileService.get(model.getName());
            pdfServices.generateImages(model.get_id(), fle, f -> {
                awsUploadService.upload(bucket, f.getName(), imageType);
                FileModel fm = fileInterface.add(f).orElseThrow();
                list.add(fm);
            });
            upload(model.getName(), model.getMimeType());
            boolean comp = pdfServices.emptyImages();
            if (comp)
                remove(model.getName());
            return list;
        });
    }

    @PostMapping("/complete/{id}")
    public CompletableFuture<FileModel> upload(@PathVariable("id") String id) {
        FileModel model = fileInterface.getById(id).orElseThrow();
        return CompletableFuture.supplyAsync(() -> {
            boolean comp = pdfServices.emptyImages();
            if (comp)
                remove(model.getName());
            return model;
        });
    }

    @DeleteMapping("/remove/{id}")
    public CompletableFuture<FileModel> delete(@PathVariable("id") String id) {
        return CompletableFuture.supplyAsync(() -> {
            FileModel model = fileInterface.removeById(id).orElseThrow();
            if (!model.isCompleted()) {
                fileService.remove(model.getName());
            }
            awsUploadService.remove(bucket, model.getName());
            return model;
        });
    }

    @PostMapping("/replace/{id}")
    public CompletableFuture<List<FileModel>> replace(@PathVariable("id") String id,
            @RequestParam("file") MultipartFile file, HttpServletRequest request) {

        Optional<FileModel> isFile = fileInterface.removeById(id);
        if (!isFile.isPresent())
            throw new NotFoundException("cannot replace file not found");

        if (!isFile.get().isCompleted()) {
            fileService.remove(isFile.get().getName());
        }

        awsUploadService.remove(bucket, isFile.get().getName());

        FileModel model = save(file, request.getHeader(originHeader));
        return CompletableFuture.supplyAsync(() -> {
            List<FileModel> list = new ArrayList<>();
            File fle = fileService.get(model.getName());
            pdfServices.generateImages(model.get_id(), fle, f -> {
                awsUploadService.upload(bucket, f.getName(), imageType);
                FileModel fm = fileInterface.add(f).orElseThrow();
                list.add(fm);
            });
            upload(model.getName(), model.getMimeType());
            boolean comp = pdfServices.emptyImages();
            if (comp)
                remove(model.getName());
            return list;
        });
    }

    private FileModel save(MultipartFile file, String origin) {
        if (file == null) {
            throw new NotFoundException(fnf);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.contains(type)) {
            throw new NotSupportedException(fnv);
        }

        FileModel fileModel = new FileModel();
        String name = fileService.uploadFile(file); // after this
        fileModel.setName(name);
        fileModel.setType("pdf");
        fileModel.setMimeType(contentType);
        fileModel.setSize(file.getSize());
        fileModel.setOrigin(origin);
        return fileInterface.add(fileModel).orElseThrow(() -> new NotFoundException(fns));
    }

    private FileModel upload(String name, String contentType) {
        awsUploadService.upload(bucket, name, contentType);
        return fileInterface.updateStatus(name, true, false)
                .orElseThrow(() -> new NotFoundException("pdf not updated while upload"));
    }

    private FileModel remove(String name) {
        boolean isCompleted = fileService.remove(name);
        return fileInterface.updateByName(name, completed, isCompleted)
                .orElseThrow(() -> new NotSupportedException("file not removed"));
    }
    // upload pdf -> uploads only 5 pages and fails what to do
}
