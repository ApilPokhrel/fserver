package com.fileserver.app.handler;

import java.util.Optional;

import com.fileserver.app.dao.file.FileInterface;
import com.fileserver.app.entity.file.FileModel;
import com.fileserver.app.entity.file.SubTypeEnum;
import com.fileserver.app.exception.NotFoundException;
import com.fileserver.app.exception.NotSupportedException;
import com.fileserver.app.service.AWSUploadService;
import com.fileserver.app.service.FFMPEGService;
import com.fileserver.app.service.FileService;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class VideoHandler {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileInterface fileInterface;

    @Autowired
    private AWSUploadService awsUploadService;

    @Autowired
    private FFMPEGService ffmpegService;

    private String type = "video";
    private String fns = "file not saved in db";
    private String bucket = "nomore";

    public FileModel save(MultipartFile file, String origin, String name, String type, String uuid) {
        FileModel fileModel = new FileModel();
        fileService.uploadFile(file, name); // after this
        fileModel.setName(name);
        fileModel.setMimeType(type);
        fileModel.setType(type);
        fileModel.setUuid(uuid);
        fileModel.setSize(file.getSize());
        fileModel.setOrigin(origin);
        return fileInterface.add(fileModel).orElseThrow(() -> new NotFoundException(fns));
    }

    public FileModel upload(String name, String contentType) {
        awsUploadService.multipartUploadSync(bucket, name, contentType);
        return fileInterface.updateStatus(name, true, false, false)
                .orElseThrow(() -> new NotFoundException("file not updated while upload"));
    }

    public FileModel complete(String name) {
        boolean isCompleted = fileService.remove(name);
        return fileInterface.updateByName(name, "completed", isCompleted)
                .orElseThrow(() -> new NotSupportedException("file not removed"));
    }

    public void remove(String name, String contentType) {
        fileService.remove(name);
        awsUploadService.remove(bucket, name);
        Optional<FileModel> preview = fileInterface.removeChild(name, contentType);
        if (preview.isPresent()) {
            fileService.remove(preview.get().getName());
            awsUploadService.remove(bucket, preview.get().getName());
        }
    }

    public FileModel preview(String parentId, String filename, String type) {
        Optional<FileModel> isPreview = fileInterface.subFile(parentId, SubTypeEnum.PREVIEW);
        FileModel previewModel;
        if (isPreview.isPresent() && fileService.exists(isPreview.get().getName())) {
            previewModel = isPreview.get();
            if (!previewModel.isUploaded()) {
                uploadPreview(previewModel.getName(), type);
            }
        } else {
            if (isPreview.isPresent())
                fileInterface.removeById(isPreview.get().get_id());
            previewModel = savePreview(parentId, filename, type);
            uploadPreview(previewModel.getName(), type);
        }
        return completePreview(previewModel.getName());
    }

    public void setProcessed(String id, boolean processed) {
        fileInterface.updateById(id, "processed", processed);
    }

    private FileModel savePreview(String id, String fileName, String fileType) {
        FileModel previewModel = new FileModel();
        String preview = ffmpegService.createPreview(fileName); // db store preview created
        previewModel.setName(preview);
        previewModel.setType(type);
        previewModel.setMimeType(fileType);
        previewModel.setParent_id(new ObjectId(id));
        previewModel.set_parent(false);
        previewModel.setSubType(SubTypeEnum.PREVIEW);
        return fileInterface.add(previewModel).orElseThrow(() -> new NotFoundException("preview not saved in db"));
    }

    private FileModel uploadPreview(String preview, String contentType) {
        awsUploadService.upload(bucket, preview, contentType); // db store preview uploaded
        return fileInterface.updateStatus(preview, true, true, false)
                .orElseThrow(() -> new NotFoundException("preview not updated while uploading"));
    }

    private FileModel completePreview(String preview) {
        boolean isCompleted = fileService.remove(preview);
        return fileInterface.updateByName(preview, "completed", isCompleted)
                .orElseThrow(() -> new NotSupportedException("preview not removed"));
    }

    public void removePreview(String name, String mimeType) {
        fileService.remove(name);
        awsUploadService.remove(bucket, name);
        Optional<FileModel> preview = fileInterface.removeChild(name, mimeType);
        if (preview.isPresent()) {
            fileService.remove(preview.get().getName());
            awsUploadService.remove(bucket, preview.get().getName());
        }
    }

}
