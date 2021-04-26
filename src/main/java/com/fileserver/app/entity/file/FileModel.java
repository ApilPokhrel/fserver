package com.fileserver.app.entity.file;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Document(collection = "files")
@CompoundIndex(name = "name", def = "{'name' : 1}")
public @Data class FileModel {

    @Id
    private String _id;
    private String name; // name of file
    private boolean is_parent = true; // does this has children
    private ObjectId parent_id; // name of parent
    private FileModel parent;
    private SubTypeEnum subType;
    private String type; // content type of file
    private String mimeType;
    private long size; // size of file
    private String idn; // anything can be hereto idenntify file version, index, quality (480p, 720p),
                        // (0, 1, 2, 5)
    private String origin; // from which origin it is uploaded
    private boolean uploaded = false; // is it uploaded to aws
    private boolean completed = false; // is it completed
    private boolean processed = false;
    private int version;
    private String path;
    private Object user;
    private ObjectId user_id;
    private String uuid;
    @JsonProperty("created_at")
    @Field("created_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date createdAt = new Date();

    @JsonProperty("updated_at")
    @Field("updated_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date updatedAt;
    private Map<String, Object> extras;

}
