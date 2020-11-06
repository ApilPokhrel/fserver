package com.fileserver.app.exception;

public class NotFoundException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 18768025L;

    public NotFoundException(String msg) {
        super(msg);
    }

}
