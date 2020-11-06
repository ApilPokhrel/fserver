package com.fileserver.app.entity.user;

import java.io.Serializable;

import lombok.Data;

public @Data class ContactModel implements Serializable{
    /**
     *
     */
    private static final long serialVersionUID = 13432424L;

    private String type;
    private String address;
    private boolean verified;
}
