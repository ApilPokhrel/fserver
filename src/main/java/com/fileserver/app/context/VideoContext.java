package com.fileserver.app.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.fileserver.app.dao.file.FileInterface;
import com.fileserver.app.entity.file.FileModel;
import com.fileserver.app.entity.file.SubTypeEnum;
import com.fileserver.app.entity.file.VideoDetail;
import com.fileserver.app.handler.VideoHandler;

import lombok.Getter;
import lombok.Setter;

public class VideoContext {
    private FileModel previewFrom;

    @Getter
    @Setter
    private List<FileModel> subs;
    private List<CompletableFuture<FileModel>> futures;

    @Getter
    @Setter
    private FileModel model;

    private VideoHandler handler;

    private FileInterface fileInterface;

    public VideoContext(FileModel model, VideoHandler handler, FileInterface fileInterface) {
        this.model = model;
        this.subs = Collections.synchronizedList(new ArrayList<>());
        this.futures = Collections.synchronizedList(new ArrayList<>());
        this.handler = handler;
        this.fileInterface = fileInterface;
    }

    public List<CompletableFuture<FileModel>> all() {
        if (!model.isUploaded()) {
            futures.add(CompletableFuture.supplyAsync(() -> handler.upload(model.get_id())));
        }

        if (!model.isProcessed()) {
            Optional<FileModel> resolution = fileInterface.subFile(model.get_id(), SubTypeEnum.RESOLUTION);
            if (resolution.isPresent()) {
                if (!resolution.get().isUploaded()) {
                    futures.add(CompletableFuture.supplyAsync(() -> handler.upload(resolution.get().get_id())));
                }

                this.previewFrom = resolution.get();
                this.subs.add(this.previewFrom);
            } else {
                VideoDetail vd = handler.detail(model.getName());
                if (vd.getHeight() > 500) {
                    this.previewFrom = handler.resolution(model.get_id(), model.getName(), model.getMimeType());
                    futures.add(CompletableFuture.supplyAsync(() -> handler.upload(this.previewFrom.get_id())));
                    this.subs.add(this.previewFrom);
                } else {
                    this.previewFrom = model;
                }
            }

            Optional<FileModel> preview = fileInterface.subFile(model.get_id(), SubTypeEnum.PREVIEW);
            if (preview.isPresent()) {
                if (!preview.get().isUploaded()) {
                    futures.add(CompletableFuture.supplyAsync(() -> handler.uploadSingle(preview.get().get_id())));
                }

                this.subs.add(preview.get());
            } else {
                FileModel p = handler.previewOnly(this.previewFrom.get_id(), this.model.getName(),
                        this.previewFrom.getMimeType(), this.previewFrom.getName());
                futures.add(CompletableFuture.supplyAsync(() -> handler.uploadSingle(p.get_id())));
                this.subs.add(p);
            }

            for (FileModel m : this.subs) {
                handler.complete(m.getName());
            }
            fileInterface.updateById(model.get_id(), "processed", true);
        }

        if (!model.isCompleted()) {
            handler.complete(model.getName());
        }
        return futures;
    }
}
