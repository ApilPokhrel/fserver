package com.fileserver.app.entity.file;

import java.util.Date;
import java.util.Map;


import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Document(collection = "files")
@CompoundIndex(name = "name", def = "{'name' : 1}")
public @Data class File {

    @Id
    private String _id;
    private String name; //name of file
    private boolean is_parent = true; //does this has children
    private String parent; //name of parent
    private String type; //content type of file
    private long size; //size of file
    private String origin; //from which origin it is uploaded
    private boolean uploaded = false; //is it uploaded to aws
    private boolean processed = false; //is it processed like using ffmpeg
    private boolean completed = false; //is it completed
    private Map<String, Object> features; //eg for video , resolution, duration

    @JsonProperty("created_at")
    @Field("created_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date createdAt = new Date();

    @JsonProperty("updated_at")
    @Field("updated_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date updatedAt;
}
