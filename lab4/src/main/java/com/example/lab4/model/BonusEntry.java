package com.example.lab4.model;

import java.math.BigDecimal;

public record BonusEntry(
        Long id,
        Long batchId,
        BigDecimal points,
        BonusStatus status
) implements Entity {}
