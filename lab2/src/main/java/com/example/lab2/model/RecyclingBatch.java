package com.example.lab2.model;

import java.math.BigDecimal;
import java.time.Instant;

public record RecyclingBatch(
        Long id,
        Long userId,
        Long pointId,
        Long typeId,
        BigDecimal weightKg,
        Instant receivedAt,
        BatchStatus status,
        BigDecimal pointsPerKg
) implements Entity {}
