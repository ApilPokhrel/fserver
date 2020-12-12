package com.fileserver.app.entity.file;

import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

public @Data class FileModelP {
    private List<FileModel> data;
    private long limit;
    private long start;
    private long total;

    @JsonIgnore
    private LinkedHashMap<Object, Object> totalCount;
}
