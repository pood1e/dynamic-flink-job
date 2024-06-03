package me.pood1e.jobmanager.service;

import me.pood1e.jobstream.common.Function;
import me.pood1e.jobstream.common.Job;
import me.pood1e.jobstream.common.Plugin;

import java.util.List;

public interface JobService extends DataService<Job, String> {
    List<Function<?>> getFunctionsByJob(String id);
    List<Plugin> getPluginsByJob(String id);
}
