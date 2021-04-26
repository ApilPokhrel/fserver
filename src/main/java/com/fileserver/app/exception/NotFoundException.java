package com.fileserver.app.exception;

public class NotFoundException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 18768025L;
    private final String message;

    public NotFoundException(String msg) {
        super(msg);
        this.message = msg;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
