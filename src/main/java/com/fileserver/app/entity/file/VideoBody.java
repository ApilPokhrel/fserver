package com.fileserver.app.entity.file;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import com.fileserver.app.config.SecurityConstants;

import lombok.Data;

public @Data class VideoBody {
    @NotBlank
    private String name;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String uuid;
    @NotBlank
    private String contentType;
    @Min(900)
    private long exp = SecurityConstants.EXPIRATION_TIME;
    private boolean replace = false;
    private boolean preview = false;
    private boolean remove = false;
    private boolean download = false;
    private String url;
    private String origin;
}
