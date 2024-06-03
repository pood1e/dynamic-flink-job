package me.pood1e.jobmanager.service.impl;

import me.pood1e.jobmanager.service.FunctionService;
import me.pood1e.jobstream.common.Function;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class FunctionServiceImpl extends MongoDataService<Function<?>, String> implements FunctionService {
    public FunctionServiceImpl(MongoTemplate mongoTemplate) {
        super("functions", Function.class, mongoTemplate);
    }
}
