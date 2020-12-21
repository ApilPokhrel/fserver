package com.fileserver.app.entity.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public @Data class VideoDetail {
    private double duration;
    private int height;
    private int width;
    private long size;
}
