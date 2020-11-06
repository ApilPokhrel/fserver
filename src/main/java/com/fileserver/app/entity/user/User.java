package com.fileserver.app.entity.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fileserver.app.dao.user.UserRolesInterface;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Document(collection = "users")
public @Data class User implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    private String _id;
    private List<ContactModel> contacts;
    private boolean verified;

    @NotEmpty
    private String password;
    private List<String> roles;
    private List<String> perms;

    private String status;

    @JsonProperty("created_at")
    @Field("created_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date createdAt = new Date();

    @JsonProperty("updated_at")
    @Field("updated_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date updatedAt;

    public List<String> queryPerms(UserRolesInterface userRole) {
        List<String> p = new ArrayList<>();
        if (this.roles != null && !this.roles.isEmpty()){
            this.roles.stream().forEach(role ->
                userRole.getByName(role).ifPresent(r -> {
                    if (r.getPerms() != null) {
                        r.getPerms().forEach(p::add);
                    }
                })
                );
        }
        return p;
    }
}
