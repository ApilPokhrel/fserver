package com.fileserver.app.entity.user;

import java.util.List;
import java.util.Map;

import lombok.Data;

public @Data class UserRoleP {

    private List<UserRoleSchema> data;
    private long limit;
    private long start;
    private long total;
    private Map<Object, Object> totalCount;

}