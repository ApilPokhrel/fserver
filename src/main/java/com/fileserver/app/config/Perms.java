package com.fileserver.app.config;

public class Perms {

    private Perms() {
        throw new IllegalStateException("Perms class");
      }

    public  static final String USER_READ = "user_read";
    public  static final String USER_WRITE = "user_write";
    public  static final String USER_DELETE = "user_delete";
    public  static final String USER_EDIT = "user_edit";
}
