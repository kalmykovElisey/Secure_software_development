package com.example.lab4.model;

import java.math.BigDecimal;

public record RecyclingType(
        Long id,
        String name,
        String unit,
        BigDecimal pointsPerKg
) implements Entity {}
