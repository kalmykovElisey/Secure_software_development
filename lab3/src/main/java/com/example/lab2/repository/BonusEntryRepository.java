package com.example.lab2.repository;

import com.example.lab2.model.BonusEntry;
import com.example.lab2.model.BonusStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class BonusEntryRepository
        implements EntityRepository<BonusEntry> {

    private static final RowMapper<BonusEntry> BONUS_MAPPER =
            (rs, rowNum) -> new BonusEntry(
                    rs.getLong("id"),
                    rs.getLong("batch_id"),
                    rs.getBigDecimal("points"),
                    BonusStatus.valueOf(rs.getString("status"))
            );

    private final JdbcTemplate jdbc;

    public BonusEntryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<BonusEntry> findAll() {
        return jdbc.query("""
                SELECT id, batch_id, points, status
                FROM public.bonus_entries
                ORDER BY id
                """, BONUS_MAPPER);
    }

    @Override
    public Optional<BonusEntry> findById(Long id) {
        return jdbc.query("""
                SELECT id, batch_id, points, status
                FROM public.bonus_entries
                WHERE id = ?
                """, BONUS_MAPPER, id)
                .stream()
                .findFirst();
    }

    @Override
    public BonusEntry save(BonusEntry bonus) {
        if (bonus.id() == null) {
            return jdbc.queryForObject("""
                    INSERT INTO public.bonus_entries
                        (batch_id, points, status)
                    VALUES (?, ?, ?)
                    RETURNING id, batch_id, points, status
                    """, BONUS_MAPPER,
                    bonus.batchId(),
                    bonus.points(),
                    bonus.status().name());
        }

        return jdbc.queryForObject("""
                UPDATE public.bonus_entries
                SET batch_id = ?, points = ?, status = ?
                WHERE id = ?
                RETURNING id, batch_id, points, status
                """, BONUS_MAPPER,
                bonus.batchId(),
                bonus.points(),
                bonus.status().name(),
                bonus.id());
    }

    @Override
    public void deleteById(Long id) {
        jdbc.update(
                "DELETE FROM public.bonus_entries WHERE id = ?",
                id
        );
    }
}