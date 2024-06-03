package me.pood1e.jobmanager.service;

import java.util.List;

public interface DataService<T, ID> {
    T create(T t);

    void update(ID id, T t);

    void delete(ID id);

    T getById(ID id);

    List<T> getByIds(List<ID> ids);

    List<T> getAll();
}
