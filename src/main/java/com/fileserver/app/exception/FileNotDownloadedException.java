package com.fileserver.app.exception;

public class FileNotDownloadedException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String message;
    private Throwable ex;

    public FileNotDownloadedException(String message, Throwable ex) {
        super(message, ex);
        this.message = message;
        this.ex = ex;
    }

    public FileNotDownloadedException(Throwable ex) {
        super(ex);
        this.ex = ex;
    }

    @Override
    public String getMessage() {
        if (!message.isBlank()) {
            return message;
        } else {
            return ex.getMessage();
        }
    }

}
