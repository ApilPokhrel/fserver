package com.fileserver.app.dao.user;

import java.util.Optional;

import com.fileserver.app.entity.user.User;

import org.springframework.stereotype.Repository;

@Repository
public interface UserInterface {

    Optional<User> add(User user);

    Optional<User> getByContact(String contact);

    Optional<User> getByContact(String type, String address);

    Optional<User> getById(String id);
}
