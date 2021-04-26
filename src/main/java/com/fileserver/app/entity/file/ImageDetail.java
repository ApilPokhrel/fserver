package com.fileserver.app.entity.file;

import lombok.Data;

public @Data class ImageDetail {
    private long width;
    private long height;
    private String mimeType;
}
