package com.example.lab2.dto;

import com.example.lab2.model.BonusEntry;
import com.example.lab2.model.RecyclingBatch;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Operations {

    private Operations() {}

    public static class WeightCorrectionRequest {
        @JsonSetter(nulls = Nulls.FAIL)
        public BigDecimal weightKg;
    }

    public static class AccrualRequest {
        @JsonSetter(nulls = Nulls.FAIL)
        public LocalDate from;

        @JsonSetter(nulls = Nulls.FAIL)
        public LocalDate to;
    }

    public record BatchWithBonus(
            RecyclingBatch batch,
            BonusEntry bonus
    ) {}

    public record AccrualResult(
            Long userId,
            int count,
            BigDecimal totalPoints,
            List<Long> bonusIds
    ) {
        public AccrualResult {
            bonusIds = List.copyOf(bonusIds);
        }
    }

    public record AvailablePoint(
            Long id,
            String address,
            BigDecimal freeWeightKg,
            BigDecimal estimatedPoints
    ) {}

    public record UserSummary(
            Long userId,
            String name,
            LocalDate from,
            LocalDate to,
            long acceptedCount,
            long cancelledCount,
            BigDecimal acceptedWeightKg,
            BigDecimal cancelledWeightKg,
            BigDecimal pendingPoints,
            BigDecimal accruedPoints,
            BigDecimal cancelledPoints
    ) {}
}