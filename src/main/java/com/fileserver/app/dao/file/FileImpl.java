package com.fileserver.app.dao.file;

import java.util.List;
import java.util.Optional;

import com.fileserver.app.entity.file.File;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class FileImpl implements FileInterface {

    @Autowired
    private MongoTemplate mTemplate;

    private String is_parent = "is_parent";

    @Override
    public Optional<File> add(File file) {
        return Optional.ofNullable(mTemplate.save(file));
    }

    @Override
    public Optional<File> getById(String id) {
        return Optional.ofNullable(mTemplate.findById(id, File.class));
    }

    @Override
    public List<File> incompletes(boolean isParent) {
        Query query = new Query(Criteria.where(is_parent).is(isParent).and("completed").is(false));
        return mTemplate.find(query, File.class);
    }

    @Override
    public Optional<File> getByName(String name) {
        Query query = new Query(Criteria.where("name").is(name));
        return Optional.ofNullable(mTemplate.findOne(query, File.class));
    }

    @Override
    public Optional<File> updateByName(String name, String key, Object value) {
        Query query = new Query(Criteria.where("name").is(name));
        Update update = new Update().set(key, value);
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        return Optional.ofNullable(mTemplate.findAndModify(query, update, options, File.class));
    }

    @Override
    public Optional<File> removeByName(String name) {
        Query query = new Query(Criteria.where("name").is(name));
        return Optional.ofNullable(mTemplate.findAndRemove(query, File.class));
    }

    @Override
    public Optional<File> removeById(String id) {
        Query query = new Query(Criteria.where("_id").is(new ObjectId(id)));
        return Optional.ofNullable(mTemplate.findAndRemove(query, File.class));
    }

    @Override
    public Optional<File> incompleted(String parent) {
        Query query = new Query(Criteria.where(is_parent).is(false).and("parent").is(parent));
        return Optional.ofNullable(mTemplate.findOne(query, File.class));
    }

    @Override
    public Optional<File> getOne(String field, String value) {
        Query query = new Query(Criteria.where(field).is(value));
        return Optional.ofNullable(mTemplate.findOne(query, File.class));
    }

    @Override
    public Optional<File> removeOne(String field, String value) {
        Query query = new Query(Criteria.where(field).is(value));
        return Optional.ofNullable(mTemplate.findAndRemove(query, File.class));
    }

    @Override
    public Optional<File> removeChild(String parent, String type) {
        Query query = new Query(Criteria.where("parent").is(parent).and(is_parent).is(true).and("type").is(type));
        return Optional.ofNullable(mTemplate.findAndRemove(query, File.class));
    }

    @Override
    public Optional<File> updateStatus(String name, boolean uploaded, boolean completed) {
        Query query = new Query(Criteria.where("name").is(name));
        Update update = new Update().set("uploaded", uploaded);
        update.set("completed", completed);
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        return Optional.ofNullable(mTemplate.findAndModify(query, update, options, File.class));
    }
}
