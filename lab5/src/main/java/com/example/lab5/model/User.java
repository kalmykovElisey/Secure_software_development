package com.example.lab5.model;

public record User(
        Long id,
        String name,
        String email
) implements Entity {}
