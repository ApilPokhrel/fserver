package com.fileserver.app.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public @Data class ErrorResponse {
    private int code;
    private String message;
    private String trace;
}
