package com.fileserver.app.exception;

public class AWSUploadException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String msg;

    public AWSUploadException(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
