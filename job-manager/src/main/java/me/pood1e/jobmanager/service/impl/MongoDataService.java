package me.pood1e.jobmanager.service.impl;

import me.pood1e.jobmanager.service.DataService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

public abstract class MongoDataService<T, ID> implements DataService<T, ID> {

    protected final MongoTemplate mongoTemplate;
    protected final String collectionName;
    protected final Class<T> tClass;

    public MongoDataService(String collectionName, Class<?> tClass, MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.collectionName = collectionName;
        this.tClass = (Class<T>) tClass;
    }

    @Override
    public T create(T t) {
        return mongoTemplate.insert(t, collectionName);
    }

    @Override
    public void update(ID id, T t) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        mongoTemplate.findAndReplace(query, t, collectionName);
    }

    @Override
    public void delete(ID id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, collectionName);
    }

    @Override
    public T getById(ID id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, tClass, collectionName);
    }

    @Override
    public List<T> getAll() {
        return mongoTemplate.findAll(tClass, collectionName);
    }

    @Override
    public List<T> getByIds(List<ID> ids) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(ids));
        return mongoTemplate.find(query, tClass, collectionName);
    }
}
