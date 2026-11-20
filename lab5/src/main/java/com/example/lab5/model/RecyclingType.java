package com.example.lab5.model;

import java.math.BigDecimal;

public record RecyclingType(
        Long id,
        String name,
        String unit,
        BigDecimal pointsPerKg
) implements Entity {}
