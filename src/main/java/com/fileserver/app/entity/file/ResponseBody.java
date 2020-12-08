package com.fileserver.app.entity.file;

import com.fileserver.app.entity.user.User;

import lombok.Data;

public @Data class ResponseBody {
    private String url;
    private FileModel file;
    private String method;
    private boolean multipart = false;
    private String token;
    private User user;
}
