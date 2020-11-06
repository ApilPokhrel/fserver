package com.fileserver.app.dao.user;

import java.util.List;
import java.util.Optional;

import com.fileserver.app.entity.user.UserRoleP;
import com.fileserver.app.entity.user.UserRoleSchema;

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
public class UserRolesImpl implements UserRolesInterface {

    @Autowired
    private MongoTemplate mTemplate;

    @Override
    public Optional<UserRoleSchema> add(UserRoleSchema user) {
        return Optional.ofNullable(mTemplate.save(user));
    }

    @Override
    public Optional<UserRoleSchema> getById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return Optional.ofNullable(mTemplate.findOne(query, UserRoleSchema.class));
    }

    @Override
    public Optional<UserRoleSchema> getByName(String name) {
        Query query = new Query(Criteria.where("name").is(name));
        return Optional.ofNullable(mTemplate.findOne(query, UserRoleSchema.class));
    }

    @Override
    public Optional<UserRoleSchema> update(String id, UserRoleSchema role) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().set("name", role.getName());
        update.set("status", role.getStatus());
        update.set("perms", role.getPerms());
        update.set("expires_at", role.getExpiresAt());
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        return Optional.ofNullable(mTemplate.findAndModify(query, update, options, UserRoleSchema.class));
    }

    @Override
    public UserRoleP list(long start, long limit, String key, String re) {
        re = re == null ? "" : re;
        MatchOperation match = Aggregation.match(Criteria.where(key).regex(re));
        LimitOperation limitOperation = Aggregation.limit(limit);
        SkipOperation skipOperation = Aggregation.skip(start);

        GroupOperation groupOperation = Aggregation.group().count().as("count");
        FacetOperation facetOperation = Aggregation.facet(match, limitOperation, skipOperation).as("data")
                .and(match, groupOperation).as("totalCount");

        Aggregation aggregation = Aggregation.newAggregation(facetOperation);
        UserRoleP user = mTemplate.aggregate(aggregation, UserRoleSchema.class, UserRoleP.class).getMappedResults()
                .get(0);
        user.setLimit(limit);
        user.setStart(start);
        user.setTotal(Long.valueOf(user.getTotalCount().get("count").toString()));
        return user;
    }

    @Override
    public Optional<UserRoleSchema> addPerms(String id, List<String> perms) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().addToSet("perms").each(perms.toArray());
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        return Optional.ofNullable(mTemplate.findAndModify(query, update, options, UserRoleSchema.class));
    }

    @Override
    public Optional<UserRoleSchema> removePerms(String id, List<String> perms) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().pullAll("perms", perms.toArray());
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        return Optional.ofNullable(mTemplate.findAndModify(query, update, options, UserRoleSchema.class));
    }

    @Override
    public Optional<UserRoleSchema> remove(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return Optional.ofNullable(mTemplate.findAndRemove(query, UserRoleSchema.class));
    }

}