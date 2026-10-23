package com.example.lab2.repository;

import com.example.lab2.model.RecyclingType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RecyclingTypeRepository
        implements EntityRepository<RecyclingType> {

    private static final RowMapper<RecyclingType> TYPE_MAPPER =
            (rs, rowNum) -> new RecyclingType(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("unit"),
                    rs.getBigDecimal("points_per_kg")
            );

    private final JdbcTemplate jdbc;

    public RecyclingTypeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<RecyclingType> findAll() {
        return jdbc.query("""
                SELECT id, name, unit, points_per_kg
                FROM public.recycling_types
                ORDER BY id
                """, TYPE_MAPPER);
    }

    @Override
    public Optional<RecyclingType> findById(Long id) {
        return jdbc.query("""
                SELECT id, name, unit, points_per_kg
                FROM public.recycling_types
                WHERE id = ?
                """, TYPE_MAPPER, id)
                .stream()
                .findFirst();
    }

    @Override
    public RecyclingType save(RecyclingType type) {
        if (type.id() == null) {
            return jdbc.queryForObject("""
                    INSERT INTO public.recycling_types
                        (name, unit, points_per_kg)
                    VALUES (?, ?, ?)
                    RETURNING id, name, unit, points_per_kg
                    """, TYPE_MAPPER,
                    type.name(), type.unit(), type.pointsPerKg());
        }

        return jdbc.queryForObject("""
                UPDATE public.recycling_types
                SET name = ?, unit = ?, points_per_kg = ?
                WHERE id = ?
                RETURNING id, name, unit, points_per_kg
                """, TYPE_MAPPER,
                type.name(), type.unit(), type.pointsPerKg(),
                type.id());
    }

    @Override
    public void deleteById(Long id) {
        jdbc.update(
                "DELETE FROM public.recycling_types WHERE id = ?",
                id
        );
    }
}