package me.pood1e.jobmanager.service.impl;

import me.pood1e.jobmanager.service.FunctionService;
import me.pood1e.jobmanager.service.JobService;
import me.pood1e.jobmanager.service.PluginService;
import me.pood1e.jobstream.common.Function;
import me.pood1e.jobstream.common.Job;
import me.pood1e.jobstream.common.Plugin;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobServiceImpl extends MongoDataService<Job, String> implements JobService {

    private final FunctionService service;
    private final PluginService pluginService;

    public JobServiceImpl(MongoTemplate mongoTemplate, FunctionService service, PluginService pluginService) {
        super("jobs", Job.class, mongoTemplate);
        this.service = service;
        this.pluginService = pluginService;
    }

    @Override
    public List<Function<?>> getFunctionsByJob(String id) {
        return service.getByIds(getById(id).getFunctions());
    }

    @Override
    public List<Plugin> getPluginsByJob(String id) {
        List<Function<?>> functions = service.getByIds(getById(id).getFunctions());
        Set<String> classes = functions.stream().map(Function::getImpl).collect(Collectors.toSet());
        return pluginService.getClassOrigin(classes);
    }
}
