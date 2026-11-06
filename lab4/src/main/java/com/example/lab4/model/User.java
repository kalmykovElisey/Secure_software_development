package com.example.lab4.model;

public record User(
        Long id,
        String name,
        String email
) implements Entity {}
