package com.fileserver.app.entity.user;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Document(collection = "user_roles")
public @Data class UserRoleSchema {

    @Id
    private String _id;
    private List<String> perms;
    private String name;
    private String status = "active";

    @JsonProperty("expires_at")
    @Field("expires_at")
    private Date expiresAt;

    @JsonProperty("created_at")
    @Field("created_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date createdAt = new Date();

}