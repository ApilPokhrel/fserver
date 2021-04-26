package com.fileserver.app.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fileserver.app.dao.file.FileInterface;
import com.fileserver.app.entity.file.FileModel;
import com.fileserver.app.entity.file.ImageDetail;
import com.fileserver.app.entity.file.Resolution;
import com.fileserver.app.entity.file.SubTypeEnum;
import com.fileserver.app.exception.NotSupportedException;
import com.fileserver.app.service.AWSUploadService;
import com.fileserver.app.service.FileService;
import com.fileserver.app.service.ImageService;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

public class ImageHandler {

    private FileService fileService;
    private FileInterface fileInterface;
    private ImageService service;
    private AWSUploadService aws;

    private String type = "image";
    private String fns = "file not saved in db";
    private String bucket = "nomore";

    @Autowired
    public ImageHandler(FileService fileService, FileInterface fileInterface, ImageService service,
            AWSUploadService aws) {
        this.fileService = fileService;
        this.fileInterface = fileInterface;
        this.service = service;
        this.aws = aws;
    }

    public FileModel save(MultipartFile file, String origin, String name, String type, String uuid) {
        FileModel fileModel = new FileModel();
        fileService.uploadFile(file, name); // after this
        Map<String, Object> extras = new HashMap<>();

        ImageDetail dImageDetail;
        try {
            dImageDetail = service.detail(name);
            extras.put("width", dImageDetail.getWidth());
            extras.put("height", dImageDetail.getHeight());
            extras.put("type", dImageDetail.getMimeType());
        } catch (IOException e) {
            e.printStackTrace();
        }

        fileModel.setName(name);
        fileModel.setMimeType(type);
        fileModel.setType(type);
        fileModel.setUuid(uuid);
        fileModel.setSize(file.getSize());
        fileModel.setOrigin(origin);
        fileModel.setExtras(extras);
        return fileInterface.add(fileModel).orElseThrow();
    }

    public List<FileModel> resize(String parentId, List<Resolution> list) {
        FileModel parent = fileInterface.getById(parentId).orElseThrow();
        List<FileModel> files = new ArrayList<>();
        for (Resolution r : list) {
            try {
                service.resize(parent.getName(), parent.getMimeType(), r.getWidth(), r.getHeight(),
                        parent.getName() + "_" + r.getLabel());
                FileModel model = new FileModel();
                model.setName(parent.getName() + "_" + r.getLabel());
                model.setType(type);
                model.setMimeType(parent.getMimeType());
                model.setParent_id(new ObjectId(parentId));
                model.set_parent(false);
                model.setSubType(SubTypeEnum.RESOLUTION);
                model.setVersion(1);
                model.setIdn(r.getLabel());
                files.add(fileInterface.add(model).orElseThrow(() -> new NotSupportedException(fns)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return files;
    }

    public FileModel upload(String id) {
        FileModel m = fileInterface.getById(id).orElseThrow();
        aws.multipartUploadSync(bucket, m.getName(), m.getMimeType());
        return fileInterface.updateById(id, "uploaded", true).orElseThrow();
    }

    public FileModel complete(String id) {
        FileModel model = fileInterface.getById(id).orElseThrow();
        if (!model.isUploaded()) {
            aws.upload(bucket, model.getName(), model.getMimeType());
            fileInterface.updateById(id, "uploaded", true);
        }
        boolean isCompleted = fileService.remove(model.getName());
        return fileInterface.updateById(id, "completed", isCompleted).orElseThrow();
    }

}
