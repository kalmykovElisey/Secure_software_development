package com.example.lab2.model;

import java.math.BigDecimal;
import java.util.Set;

public record CollectionPoint(
        Long id,
        String address,
        Set<Long> recyclingTypeIds,
        Boolean available,
        BigDecimal dailyWeightLimit
) implements Entity {

    public CollectionPoint {
        recyclingTypeIds = Set.copyOf(recyclingTypeIds);
    }
}