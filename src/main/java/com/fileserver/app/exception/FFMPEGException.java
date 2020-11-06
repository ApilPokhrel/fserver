package com.fileserver.app.exception;

public class FFMPEGException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String msg;

    public FFMPEGException(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

}
