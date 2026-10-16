package com.example.lab2.model;

import java.math.BigDecimal;

public record BonusEntry(
        Long id,
        Long batchId,
        BigDecimal points,
        BonusStatus status
) implements Entity {}