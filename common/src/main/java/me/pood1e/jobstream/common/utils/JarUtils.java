package me.pood1e.jobstream.common.utils;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@UtilityClass
public class JarUtils {
    public static Set<String> getClassNamesFromJarFile(File jar, Set<String> packages) {
        Set<String> classNames = new HashSet<>();
        try (JarFile jarFile = new JarFile(jar)) {
            Enumeration<JarEntry> e = jarFile.entries();
            while (e.hasMoreElements()) {
                JarEntry jarEntry = e.nextElement();
                if (jarEntry.getName().endsWith(".class")) {
                    String className = jarEntry.getName()
                            .replace("/", ".")
                            .replace(".class", "");
                    int index = className.lastIndexOf('.');
                    String packageName = className.substring(0, index >= 0 ? index : className.length());
                    if (packages.contains(packageName)) {
                        classNames.add(className);
                    }
                }
            }
            return classNames;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static URLClassLoader getClassLoaderFromUrls(List<URL> url) {
        return URLClassLoader.newInstance(url.toArray(new URL[0]));
    }

    public static URLClassLoader getClassLoaderFromJarFile(File jar) throws MalformedURLException {
        return URLClassLoader.newInstance(new URL[]{jar.toURI().toURL()});
    }
}
