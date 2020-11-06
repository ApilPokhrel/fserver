package com.fileserver.app.dao.user;

import java.util.List;
import java.util.Optional;

import com.fileserver.app.entity.user.UserRoleP;
import com.fileserver.app.entity.user.UserRoleSchema;

import org.springframework.stereotype.Repository;

@Repository
public interface UserRolesInterface  {

    public Optional<UserRoleSchema> getById(String id);

    public Optional<UserRoleSchema> getByName(String name);

    public Optional<UserRoleSchema> add(UserRoleSchema user);

    public Optional<UserRoleSchema> update(String id, UserRoleSchema role);

    public UserRoleP list(long start, long limit, String key, String re);

    public Optional<UserRoleSchema> addPerms(String id, List<String> perms);

    public Optional<UserRoleSchema> removePerms(String id, List<String> perms);

    public Optional<UserRoleSchema> remove(String id);

}