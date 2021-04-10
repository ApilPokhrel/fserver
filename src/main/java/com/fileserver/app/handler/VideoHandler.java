package com.fileserver.app.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fileserver.app.dao.file.FileInterface;
import com.fileserver.app.entity.file.FileModel;
import com.fileserver.app.entity.file.SubTypeEnum;
import com.fileserver.app.entity.file.VideoDetail;
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
        VideoDetail vd = detail(name);
        Map<String, Object> extras = new HashMap<>();
        extras.put("duration", vd.getDuration());
        extras.put("width", vd.getWidth());
        extras.put("height", vd.getHeight());
        extras.put("type", vd.getMimeType());
        fileModel.setName(name);
        fileModel.setMimeType(type);
        fileModel.setType(type);
        fileModel.setUuid(uuid);
        fileModel.setSize(file.getSize());
        fileModel.setOrigin(origin);
        fileModel.setExtras(extras);
        return fileInterface.add(fileModel).orElseThrow(() -> new NotFoundException(fns));
    }

    public FileModel save(String origin, String name, String type, String uuid) {
        FileModel fileModel = new FileModel();
        VideoDetail vd = detail(name);
        Map<String, Object> extras = new HashMap<>();
        extras.put("duration", vd.getDuration());
        extras.put("width", vd.getWidth());
        extras.put("height", vd.getHeight());
        extras.put("type", vd.getMimeType());
        fileModel.setName(name);
        fileModel.setMimeType(type);
        fileModel.setType(type);
        fileModel.setUuid(uuid);
        fileModel.setSize(vd.getSize());
        fileModel.setOrigin(origin);
        fileModel.setExtras(extras);
        return fileInterface.add(fileModel).orElseThrow(() -> new NotFoundException(fns));
    }

    public FileModel resolution(String parent, String name, String contextType) {
        FileModel model = new FileModel();
        String res = ffmpegService.smallDrop(name, "-1:480", "480p"); // db store preview created
        model.setName(res);
        model.setType(type);
        model.setMimeType(contextType);
        model.setParent_id(new ObjectId(parent));
        model.set_parent(false);
        model.setSubType(SubTypeEnum.RESOLUTION);
        model.setVersion(1);
        model.setIdn("480p");
        return fileInterface.add(model).orElseThrow(() -> new NotFoundException("preview not saved in db"));

    }

    public FileModel upload(String name, String contentType) {
        awsUploadService.multipartUploadSync(bucket, name, contentType);
        return fileInterface.updateStatus(name, true, false, false)
                .orElseThrow(() -> new NotFoundException("file not updated while upload"));
    }

    public FileModel upload(String id) {
        FileModel m = fileInterface.getById(id).orElseThrow();
        awsUploadService.multipartUploadSync(bucket, m.getName(), m.getMimeType());
        return fileInterface.updateStatus(m.getName(), true, false, false)
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

    public void removeAll(FileModel model) {
        fileService.remove(model.getName());
        awsUploadService.remove(bucket, model.getName());
        List<FileModel> subFiles = fileInterface.listSubFile(model.get_id());
        for (FileModel sb : subFiles) {
            fileService.remove(sb.getName());
            awsUploadService.remove(bucket, sb.getName());
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
            previewModel = savePreview(parentId, filename, type, filename);
            uploadPreview(previewModel.getName(), type);
        }
        return completePreview(previewModel.getName());
    }

    public FileModel previewOnly(String parentId, String output, String type, String input) {
        Optional<FileModel> isPreview = fileInterface.subFile(parentId, SubTypeEnum.PREVIEW);
        FileModel previewModel;
        if (isPreview.isPresent() && fileService.exists(isPreview.get().getName())) {
            previewModel = isPreview.get();
        } else {
            if (isPreview.isPresent())
                fileInterface.removeById(isPreview.get().get_id());
            previewModel = savePreview(parentId, output, type, input);
        }
        return previewModel;
    }

    public void setProcessed(String id, boolean processed) {
        fileInterface.updateById(id, "processed", processed);
    }

    private FileModel savePreview(String id, String output, String fileType, String input) {
        FileModel previewModel = new FileModel();
        String preview = ffmpegService.createPreview(input, output); // db store preview created
        previewModel.setName(preview);
        previewModel.setType(type);
        previewModel.setMimeType(fileType);
        previewModel.setParent_id(new ObjectId(id));
        previewModel.set_parent(false);
        previewModel.setSubType(SubTypeEnum.PREVIEW);
        return fileInterface.add(previewModel).orElseThrow(() -> new NotFoundException("preview not saved in db"));
    }

    public FileModel uploadPreview(String preview, String contentType) {
        awsUploadService.upload(bucket, preview, contentType); // db store preview uploaded
        return fileInterface.updateStatus(preview, true, true, false)
                .orElseThrow(() -> new NotFoundException("preview not updated while uploading"));
    }

    public FileModel uploadSingle(String id) {
        FileModel m = fileInterface.getById(id).orElseThrow();
        awsUploadService.upload(bucket, m.getName(), m.getMimeType());
        return fileInterface.updateStatus(m.getName(), true, false, false).orElseThrow();
    }

    public FileModel completePreview(String preview) {
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

    public VideoDetail detail(String name) {
        return ffmpegService.detail(name);
    }

}
