package me.pood1e.jobmanager.service.impl;

import lombok.extern.slf4j.Slf4j;
import me.pood1e.jobmanager.service.PluginService;
import me.pood1e.jobstream.common.FunctionType;
import me.pood1e.jobstream.common.Plugin;
import me.pood1e.jobstream.common.PluginClass;
import me.pood1e.jobstream.common.utils.JarUtils;
import me.pood1e.jobstream.pluginbase.Mark;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class PluginServiceImpl extends MongoDataService<Plugin, String> implements PluginService {
    private final String location;

    public PluginServiceImpl(MongoTemplate mongoTemplate, @Value("${job.location}") String location) {
        super("plugins", Plugin.class, mongoTemplate);
        this.location = location;
    }

    @Override
    public void addPluginJar(String id, MultipartFile file) {
        Plugin plugin = getById(id);
        if (plugin != null) {
            saveJar(id, file);
            checkAndSaveJar(plugin);
        }
    }

    private void saveJar(String id, MultipartFile file) {
        try {
            file.transferTo(getTempJar(id));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(String id, Plugin plugin) {
        Query query = new Query();
        Set<String> classes = plugin.getClasses() == null ? Set.of() : plugin.getClasses().stream().map(PluginClass::getClassName).collect(Collectors.toSet());
        query.addCriteria(Criteria.where("_id").ne(id).and("classes.className").in(classes));
        if (mongoTemplate.exists(query, Plugin.class, collectionName)) {
            throw new RuntimeException();
        }
        super.update(id, plugin);
    }

    private void checkAndSaveJar(Plugin plugin) {
        File jar = getTempJar(plugin.getId());
        if (!jar.exists()) {
            return;
        }
        plugin.setJarHash(hashFile(jar));
        Set<String> classes = JarUtils.getClassNamesFromJarFile(jar, new HashSet<>(plugin.getPackages()));
        try (URLClassLoader classLoader = JarUtils.getClassLoaderFromJarFile(jar)) {
            List<PluginClass> pluginClasses = getPluginFunctions(classes, classLoader);
            plugin.setClasses(pluginClasses);
            update(plugin.getId(), plugin);
            jar.renameTo(getJarFile(plugin.getId()));
        } catch (IOException | ClassNotFoundException e) {
            jar.delete();
            throw new RuntimeException(e);
        }
    }

    private List<PluginClass> getPluginFunctions(Set<String> classNames, URLClassLoader classLoader) throws ClassNotFoundException {
        List<PluginClass> functions = new ArrayList<>();
        for (String name : classNames) {
            Class<?> clazz = classLoader.loadClass(name);
            Optional<FunctionType> type = Arrays.stream(FunctionType.values()).filter(functionType -> functionType.getBaseClass().isAssignableFrom(clazz)).findAny();
            Mark mark = clazz.getDeclaredAnnotation(Mark.class);
            functions.add(PluginClass.builder()
                    .className(name)
                    .name(mark == null ? name : mark.value())
                    .type(type.orElse(null))
                    .build());
        }
        return functions;
    }

    @Override
    public File getJarFile(String id) {
        return new File(location, id + ".jar");
    }

    public File getTempJar(String id) {
        File dir = new File(location);
        if (!dir.exists() && dir.mkdirs()) {
            log.info("mkdir directory successfully");
        }
        return new File(location, "tmp_" + id + ".jar");
    }

    @Override
    public Plugin getClassOrigin(String className) {
        Query query = new Query();
        query.addCriteria(Criteria.where("classes.className").is(className));
        return mongoTemplate.findOne(query, Plugin.class, collectionName);
    }

    @Override
    public List<Plugin> getClassOrigin(Set<String> classes) {
        Query query = new Query();
        query.addCriteria(Criteria.where("classes.className").in(classes));
        return mongoTemplate.find(query, Plugin.class, collectionName);
    }

    @Override
    public List<PluginClass> getPluginClasses() {
        return getAll().stream().flatMap(p -> p.getClasses() != null ? p.getClasses().stream() : Stream.empty()).collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        File jarFile = getJarFile(id);
        if (jarFile.exists() && jarFile.delete()) {
            log.info("delete jar successfully");
        }
        super.delete(id);
    }

    private String hashFile(File jar) {
        try {
            return Hex.encodeHexString(DigestUtils.digest(MessageDigest.getInstance("md5"), jar));
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
