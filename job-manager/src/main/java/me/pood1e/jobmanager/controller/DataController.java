package me.pood1e.jobmanager.controller;

import me.pood1e.jobmanager.service.DataService;
import me.pood1e.jobstream.common.Function;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public abstract class DataController<T, ID> {

    protected final DataService<T, ID> dataService;

    protected DataController(DataService<T, ID> dataService) {
        this.dataService = dataService;
    }

    @PostMapping
    public T create(@RequestBody T t) {
        return dataService.create(t);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable ID id, @RequestBody T t) {
        dataService.update(id, t);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable ID id) {
        dataService.delete(id);
    }

    @GetMapping("/{id}")
    public T getById(@PathVariable ID id) {
        return dataService.getById(id);
    }

    @GetMapping
    public List<T> getAll() {
        return dataService.getAll();
    }

    @CrossOrigin
    @RestController
    @RequestMapping("/function")
    public static class FunctionController extends DataController<Function<?>, String> {
        protected FunctionController(DataService<Function<?>, String> dataService) {
            super(dataService);
        }
    }
}
