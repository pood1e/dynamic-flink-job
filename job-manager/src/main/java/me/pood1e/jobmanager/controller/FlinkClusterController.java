package me.pood1e.jobmanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import me.pood1e.jobstream.common.ClusterInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/cluster")
public class FlinkClusterController {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${cluster.web}")
    private String web;

    @GetMapping("/info")
    public ClusterInfo getClusterInfo() {
        JsonNode response = restTemplate.getForObject(web + "/taskmanagers", JsonNode.class);
        int workers = response.get("taskmanagers").size();
        return ClusterInfo.builder().workers(workers).build();
    }
}
