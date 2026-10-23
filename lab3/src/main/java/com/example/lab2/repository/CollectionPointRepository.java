package com.example.lab2.repository;

import com.example.lab2.model.CollectionPoint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class CollectionPointRepository
        implements EntityRepository<CollectionPoint> {

    private static final String SELECT_POINTS = """
            SELECT p.id, p.address, p.available, p.daily_weight_limit,
                   ARRAY(
                       SELECT pt.type_id
                       FROM public.collection_point_types pt
                       WHERE pt.point_id = p.id
                       ORDER BY pt.type_id
                   ) AS type_ids
            FROM public.collection_points p
            """;

    private static final RowMapper<CollectionPoint> POINT_MAPPER =
            (rs, rowNum) -> {
                Array array = rs.getArray("type_ids");
                Set<Long> typeIds;

                try {
                    Long[] values = (Long[]) array.getArray();
                    typeIds = Set.copyOf(Arrays.asList(values));
                } finally {
                    array.free();
                }

                return new CollectionPoint(
                        rs.getLong("id"),
                        rs.getString("address"),
                        typeIds,
                        rs.getBoolean("available"),
                        rs.getBigDecimal("daily_weight_limit")
                );
            };

    private final JdbcTemplate jdbc;

    public CollectionPointRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CollectionPoint> findAll() {
        return jdbc.query(
                SELECT_POINTS + " ORDER BY p.id",
                POINT_MAPPER
        );
    }

    @Override
    public Optional<CollectionPoint> findById(Long id) {
        return jdbc.query(
                SELECT_POINTS + " WHERE p.id = ?",
                POINT_MAPPER,
                id
        ).stream().findFirst();
    }

    @Override
    @Transactional
    public CollectionPoint save(CollectionPoint point) {
        Long id;

        if (point.id() == null) {
            id = jdbc.queryForObject("""
                    INSERT INTO public.collection_points
                        (address, available, daily_weight_limit)
                    VALUES (?, ?, ?)
                    RETURNING id
                    """, Long.class,
                    point.address(),
                    point.available(),
                    point.dailyWeightLimit());
        } else {
            id = jdbc.queryForObject("""
                    UPDATE public.collection_points
                    SET address = ?,
                        available = ?,
                        daily_weight_limit = ?
                    WHERE id = ?
                    RETURNING id
                    """, Long.class,
                    point.address(),
                    point.available(),
                    point.dailyWeightLimit(),
                    point.id());
        }

        jdbc.update("""
                DELETE FROM public.collection_point_types
                WHERE point_id = ?
                """, id);

        for (Long typeId : point.recyclingTypeIds()) {
            jdbc.update("""
                    INSERT INTO public.collection_point_types
                        (point_id, type_id)
                    VALUES (?, ?)
                    """, id, typeId);
        }

        return new CollectionPoint(
                id,
                point.address(),
                point.recyclingTypeIds(),
                point.available(),
                point.dailyWeightLimit()
        );
    }

    @Override
    public void deleteById(Long id) {
        jdbc.update(
                "DELETE FROM public.collection_points WHERE id = ?",
                id
        );
    }
}