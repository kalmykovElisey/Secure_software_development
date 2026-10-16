package com.example.lab2.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.math.BigDecimal;
import java.util.Set;
import com.example.lab2.model.BatchStatus;
import com.example.lab2.model.BonusStatus;

public final class Requests {

    private Requests() {}

    public static class UserRequest {
        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public String name;

        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public String email;
    }

    public static class TypeRequest {
        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public String name;

        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public String unit;

        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public BigDecimal pointsPerKg;
    }

    public static class PointRequest {
        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public String address;

        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public Set<Long> recyclingTypeIds;

        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public Boolean available;

        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public BigDecimal dailyWeightLimit;
    }

    public static class BatchRequest {
        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public Long userId;

        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public Long pointId;

        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public Long typeId;

        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public BigDecimal weightKg;

        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public BatchStatus status;
    }

    public static class BonusRequest {
        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public Long batchId;

        @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
        public BonusStatus status;
    }
}