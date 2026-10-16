package com.example.lab2.model;

public record User(
        Long id,
        String name,
        String email
) implements Entity {}