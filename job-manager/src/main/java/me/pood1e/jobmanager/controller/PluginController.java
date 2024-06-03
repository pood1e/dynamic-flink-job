package me.pood1e.jobmanager.controller;

import me.pood1e.jobmanager.service.PluginService;
import me.pood1e.jobstream.common.Plugin;
import me.pood1e.jobstream.common.PluginClass;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/plugin")
public class PluginController extends DataController<Plugin, String> {

    private final PluginService service;

    protected PluginController(PluginService service, PluginService service1) {
        super(service);
        this.service = service1;
    }

    @PostMapping("/{id}/jar")
    public void upload(@PathVariable String id, @RequestParam(value = "file") MultipartFile file) {
        service.addPluginJar(id, file);
    }

    @GetMapping("/{id}/jar")
    public ResponseEntity<Resource> downloadPluginJar(@PathVariable String id) throws FileNotFoundException {
        File file = service.getJarFile(id);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/class")
    public Plugin queryByClass(@RequestParam String className) {
        return service.getClassOrigin(className);
    }

    @GetMapping("/classes")
    public List<PluginClass> getAllClasses() {
        return service.getPluginClasses();
    }
}
