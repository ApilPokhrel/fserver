package com.fileserver.app.exception;

public class NotSupportedException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String msg;

    public NotSupportedException(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
