package me.pood1e.jobstream.configjob.core.utils;

import lombok.experimental.UtilityClass;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@UtilityClass
public class ReflectUtils {

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getSuperGenericType(Class<?> clazz, Class<?> target, int index) {
        ParameterizedType parameterizedType = (ParameterizedType) clazz.getGenericSuperclass();
        Type type = parameterizedType.getRawType();
        if (type.equals(target)) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            return (Class<T>) actualTypeArguments[index];
        } else {
            return getSuperGenericType(clazz.getSuperclass(), target, index);
        }
    }


    public static <T> Class<T> getInterfaceGenericType(Class<?> clazz, Class<?> target, int index) {
        Class<T> tClass = findInterfaceGenericType(clazz, target, index);
        if (tClass == null) {
            throw new RuntimeException();
        }
        return tClass;
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> findInterfaceGenericType(Class<?> clazz, Class<?> target, int index) {
        if (clazz.equals(Object.class) || clazz.equals(Class.class)) {
            return null;
        }
        for (Type type : clazz.getGenericInterfaces()) {
            if (!(type instanceof ParameterizedType parameterizedType)) {
                continue;
            }
            Type typeClass = parameterizedType.getRawType();
            if (typeClass.equals(target)) {
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments[index] instanceof Class<?> ret) {
                    return (Class<T>) ret;
                } else if (actualTypeArguments[index] instanceof ParameterizedType type1) {
                    return (Class<T>) type1.getRawType();
                }
            } else {
                return findInterfaceGenericType((Class<?>) typeClass, target, index);
            }
        }
        return null;
    }
}
