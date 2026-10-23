package com.example.lab2.repository;

import com.example.lab2.model.BatchStatus;
import com.example.lab2.model.RecyclingBatch;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class RecyclingBatchRepository
        implements EntityRepository<RecyclingBatch> {

    private static final String SELECT_BATCHES = """
            SELECT id, user_id, point_id, type_id, weight_kg,
                   received_at, status, points_per_kg
            FROM public.recycling_batches
            """;

    private static final RowMapper<RecyclingBatch> BATCH_MAPPER =
            (rs, rowNum) -> new RecyclingBatch(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getLong("point_id"),
                    rs.getLong("type_id"),
                    rs.getBigDecimal("weight_kg"),
                    rs.getObject(
                            "received_at", OffsetDateTime.class
                    ).toInstant(),
                    BatchStatus.valueOf(rs.getString("status")),
                    rs.getBigDecimal("points_per_kg")
            );

    private final JdbcTemplate jdbc;

    public RecyclingBatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<RecyclingBatch> findAll() {
        return jdbc.query(
                SELECT_BATCHES + " ORDER BY id",
                BATCH_MAPPER
        );
    }

    @Override
    public Optional<RecyclingBatch> findById(Long id) {
        return jdbc.query(
                SELECT_BATCHES + " WHERE id = ?",
                BATCH_MAPPER,
                id
        ).stream().findFirst();
    }

    @Override
    public RecyclingBatch save(RecyclingBatch batch) {
        OffsetDateTime receivedAt =
                batch.receivedAt().atOffset(ZoneOffset.UTC);

        if (batch.id() == null) {
            return jdbc.queryForObject("""
                    INSERT INTO public.recycling_batches
                        (user_id, point_id, type_id, weight_kg,
                         received_at, status, points_per_kg)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    RETURNING id, user_id, point_id, type_id,
                              weight_kg, received_at, status,
                              points_per_kg
                    """, BATCH_MAPPER,
                    batch.userId(),
                    batch.pointId(),
                    batch.typeId(),
                    batch.weightKg(),
                    receivedAt,
                    batch.status().name(),
                    batch.pointsPerKg());
        }

        return jdbc.queryForObject("""
                UPDATE public.recycling_batches
                SET user_id = ?,
                    point_id = ?,
                    type_id = ?,
                    weight_kg = ?,
                    received_at = ?,
                    status = ?,
                    points_per_kg = ?
                WHERE id = ?
                RETURNING id, user_id, point_id, type_id,
                          weight_kg, received_at, status,
                          points_per_kg
                """, BATCH_MAPPER,
                batch.userId(),
                batch.pointId(),
                batch.typeId(),
                batch.weightKg(),
                receivedAt,
                batch.status().name(),
                batch.pointsPerKg(),
                batch.id());
    }

    @Override
    public void deleteById(Long id) {
        jdbc.update(
                "DELETE FROM public.recycling_batches WHERE id = ?",
                id
        );
    }
}