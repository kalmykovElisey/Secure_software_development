package com.example.lab5.model;

import java.math.BigDecimal;

public record BonusEntry(
        Long id,
        Long batchId,
        BigDecimal points,
        BonusStatus status
) implements Entity {}
