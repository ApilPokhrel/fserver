package com.fileserver.app.controller;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;

import com.fileserver.app.dao.file.FileInterface;
import com.fileserver.app.entity.file.File;
import com.fileserver.app.exception.NotFoundException;
import com.fileserver.app.exception.NotSupportedException;
import com.fileserver.app.service.AWSUploadService;
import com.fileserver.app.service.FFMPEGService;
import com.fileserver.app.service.FileService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/file")
public class FileController {

    private FileService fileService;
    private FFMPEGService ffmpegService;
    private AWSUploadService awsUploadService;
    private FileInterface fileInterface;

    String completed = "completed";
    String uploaded = "uploaded";

    @Autowired
    public FileController(FileService fileService, FFMPEGService ffmpegService, AWSUploadService awsUploadService,
            FileInterface fileInterface) {
        this.fileService = fileService;
        this.ffmpegService = ffmpegService;
        this.awsUploadService = awsUploadService;
        this.fileInterface = fileInterface;
    }

    @PostMapping("/upload/video")
    public CompletableFuture<Object> uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
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
            if(!fileService.exists(fileModel.getName())){
                fileService.uploadFile(file); // if not in directory upload file
                fileInterface.updateByName(fileModel.getName(), uploaded, false);
                fileInterface.updateByName(fileModel.getName(), completed, false);
            }
        }
        final boolean is_uploaded = fileModel.isUploaded();
        return CompletableFuture.supplyAsync(() -> {
            if (!is_uploaded) {
                return awsUploadService.multipartUpload("nomore", name, contentType).thenApplyAsync(f -> {
                    Optional<File> fm = fileInterface.updateByName(name, uploaded, true);
                    preview(fm.get());
                    return fm.get();
                });

            } else {
                return fileInterface.getByName(name).get();
            }
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

    private File preview(File d){
        Optional<File> isPreview = fileInterface.incompleted(d.getName());
        File previewModel = new File();
        if(!isPreview.isPresent()){
            String preview = ffmpegService.createPreview(d.getName()); //db store preview created
            previewModel.setName(preview);
            previewModel.setType(d.getType());
            previewModel.setParent(d.getName());
            previewModel.set_parent(false);
            previewModel = fileInterface.add(previewModel).orElseThrow(() -> new NotFoundException("sub-file not saved in db"));
        } else {
            previewModel = isPreview.get();
            if(!fileService.exists(previewModel.getName())){
                ffmpegService.createPreview(d.getName()); // if not in directory upload file
                fileInterface.updateByName(previewModel.getName(), uploaded, false);
                fileInterface.updateByName(previewModel.getName(), completed, false);
            }
        }

        if(!previewModel.isUploaded()){
            awsUploadService.upload("nomore", previewModel.getName(), "video/mp4"); //db store preview uploaded
            fileInterface.updateByName(previewModel.getName(), uploaded, true);
            boolean isCompleted = fileService.remove(previewModel.getName());
            fileInterface.updateByName(previewModel.getName(), completed, isCompleted);
        } else if(!previewModel.isCompleted()){
            boolean isCompleted = fileService.remove(previewModel.getName());
            fileInterface.updateByName(previewModel.getName(), completed, isCompleted);
        }

        if(!d.isCompleted()) {
            boolean isCompleted = fileService.remove(d.getName());
            fileInterface.updateByName(d.getName(), completed, isCompleted);
         }
        return d;
    }
}
