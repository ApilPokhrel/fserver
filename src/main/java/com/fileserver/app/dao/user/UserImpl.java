package com.fileserver.app.dao.user;

import java.util.Optional;

import com.fileserver.app.entity.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class UserImpl implements UserInterface {

    @Autowired
    private MongoTemplate mTemplate;

    @Override
    public Optional<User> add(User user) {
        return Optional.ofNullable(mTemplate.save(user));
    }

    @Override
    public Optional<User> getByContact(String contact) {
        Query query = new Query(Criteria.where("contacts.address").is(contact));
        return Optional.ofNullable(mTemplate.findOne(query, User.class));
    }

    @Override
    public Optional<User> getByContact(String type, String address) {
        Query query = new Query(Criteria.where("contacts.type").is(type).and("contacts.address").is(address));
        return Optional.ofNullable(mTemplate.findOne(query, User.class));
    }

    @Override
    public Optional<User> getById(String id) {
        return Optional.ofNullable(mTemplate.findById(id, User.class));
    }

}
