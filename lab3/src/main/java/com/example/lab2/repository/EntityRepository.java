package com.example.lab2.repository;

import com.example.lab2.model.Entity;

import java.util.List;
import java.util.Optional;

public interface EntityRepository<T extends Entity> {

    List<T> findAll();

    Optional<T> findById(Long id);

    T save(T entity);

    void deleteById(Long id);
}