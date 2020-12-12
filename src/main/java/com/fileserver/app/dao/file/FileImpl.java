package com.fileserver.app.dao.file;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fileserver.app.entity.file.FileModel;
import com.fileserver.app.entity.file.FileModelP;
import com.fileserver.app.entity.file.SubTypeEnum;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
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
    public Optional<FileModel> add(FileModel file) {
        return Optional.ofNullable(mTemplate.save(file));
    }

    @Override
    public Optional<FileModel> getById(String id) {
        return Optional.ofNullable(mTemplate.findById(id, FileModel.class));
    }

    @Override
    public Optional<FileModel> getByName(String name) {
        Query query = new Query(Criteria.where("name").is(name));
        return Optional.ofNullable(mTemplate.findOne(query, FileModel.class));
    }

    @Override
    public Optional<FileModel> updateByName(String name, String key, Object value) {
        Query query = new Query(Criteria.where("name").is(name));
        Update update = new Update().set(key, value);
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        return Optional.ofNullable(mTemplate.findAndModify(query, update, options, FileModel.class));
    }

    @Override
    public Optional<FileModel> removeByName(String name) {
        Query query = new Query(Criteria.where("name").is(name));
        return Optional.ofNullable(mTemplate.findAndRemove(query, FileModel.class));
    }

    @Override
    public Optional<FileModel> removeById(String id) {
        Query query = new Query(Criteria.where("_id").is(new ObjectId(id)));
        return Optional.ofNullable(mTemplate.findAndRemove(query, FileModel.class));
    }

    @Override
    public Optional<FileModel> getOne(String field, String value) {
        Query query = new Query(Criteria.where(field).is(value));
        return Optional.ofNullable(mTemplate.findOne(query, FileModel.class));
    }

    @Override
    public Optional<FileModel> removeOne(String field, String value) {
        Query query = new Query(Criteria.where(field).is(value));
        return Optional.ofNullable(mTemplate.findAndRemove(query, FileModel.class));
    }

    @Override
    public Optional<FileModel> removeChild(String parent, String type) {
        Query query = new Query(Criteria.where("parent").is(parent).and(is_parent).is(true).and("mimeType").is(type));
        return Optional.ofNullable(mTemplate.findAndRemove(query, FileModel.class));
    }

    @Override
    public Optional<FileModel> updateStatus(String name, boolean uploaded, boolean processed, boolean completed) {
        Query query = new Query(Criteria.where("name").is(name));
        Update update = new Update().set("uploaded", uploaded);
        update.set("completed", completed);
        update.set("processed", processed);
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        return Optional.ofNullable(mTemplate.findAndModify(query, update, options, FileModel.class));
    }

    @Override
    public Optional<FileModel> subFile(String parentId, SubTypeEnum subType) {
        Query query = new Query(Criteria.where("parent_id").is(new ObjectId(parentId)).and("subType").is(subType)
                .and(is_parent).is(false));
        return Optional.ofNullable(mTemplate.findOne(query, FileModel.class));
    }

    @Override
    public Optional<FileModel> updateById(String id, String key, Object value) {
        Query query = new Query(Criteria.where("_id").is(new ObjectId(id)));
        Update update = new Update().set(key, value);
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        return Optional.ofNullable(mTemplate.findAndModify(query, update, options, FileModel.class));
    }

    @Override
    public List<FileModel> listSubFile(String parentId) {
        Query query = new Query(Criteria.where("parent_id").is(new ObjectId(parentId)).and(is_parent).is(false));
        return mTemplate.find(query, FileModel.class);
    }

    @Override
    public FileModelP list(long start, long limit, Map<String, String> q) {
        Criteria criteria = new Criteria();

        if (q != null) {
            Set<String> keys = q.keySet();
            for (String k : keys) {
                criteria.and(k).is(q.get(k));
            }
        }
        MatchOperation match = Aggregation.match(criteria);
        LimitOperation limitOperation = Aggregation.limit(limit);
        SkipOperation skipOperation = Aggregation.skip(start);

        GroupOperation groupOperation = Aggregation.group().count().as("count");
        FacetOperation facetOperation = Aggregation.facet(match, limitOperation, skipOperation).as("data")
                .and(match, groupOperation).as("totalCount");

        Aggregation aggregation = Aggregation.newAggregation(facetOperation);
        FileModelP model = mTemplate.aggregate(aggregation, FileModel.class, FileModelP.class).getMappedResults()
                .get(0);
        model.setLimit(limit);
        model.setStart(start);
        model.setTotal(Long.valueOf(model.getTotalCount().get("count").toString()));
        return model;
    }
}
