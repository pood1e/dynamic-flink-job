package me.pood1e.jobmanager.service;

import me.pood1e.jobstream.common.Plugin;
import me.pood1e.jobstream.common.PluginClass;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Set;

public interface PluginService extends DataService<Plugin, String> {

    void addPluginJar(String id, MultipartFile file);

    File getJarFile(String id);

    Plugin getClassOrigin(String className);

    List<Plugin> getClassOrigin(Set<String> classes);

    List<PluginClass> getPluginClasses();
}
