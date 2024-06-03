package me.pood1e.jobmanager.controller;

import me.pood1e.jobmanager.service.JobService;
import me.pood1e.jobstream.common.Function;
import me.pood1e.jobstream.common.Job;
import me.pood1e.jobstream.common.Plugin;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/job")
public class JobController extends DataController<Job, String> {
    private final JobService jobService;

    protected JobController(JobService jobService, JobService jobService1) {
        super(jobService);
        this.jobService = jobService1;
    }

    @GetMapping("/{id}/functions")
    public List<Function<?>> getFunctions(@PathVariable String id) {
        return jobService.getFunctionsByJob(id);
    }

    @GetMapping("/{id}/plugins")
    public List<Plugin> getPlugins(@PathVariable String id) {
        return jobService.getPluginsByJob(id);
    }
}
