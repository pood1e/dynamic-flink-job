package me.pood1e.jobstream.configjob.core.utils;

import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader {
    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public Class<?> findOrLoadClass(String name) throws ClassNotFoundException {
//        if (findLoadedClass(name) == null) {
//            return super.loadClass(name);
//        }
        return super.loadClass(name);
    }
}
