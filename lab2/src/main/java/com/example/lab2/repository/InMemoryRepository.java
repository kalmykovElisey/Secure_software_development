package com.example.lab2.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.example.lab2.model.Entity;

public abstract class InMemoryRepository<T extends Entity> {

    private final Map<Long, T> storage = new LinkedHashMap<>();
    private long nextId = 1;

    public long nextId() {
        return nextId++;
    }

    public List<T> findAll() {
        return List.copyOf(storage.values());
    }

    public Optional<T> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public T save(T entity) {
        storage.put(entity.id(), entity);
        return entity;
    }

    public void deleteById(Long id) {
        storage.remove(id);
    }
}