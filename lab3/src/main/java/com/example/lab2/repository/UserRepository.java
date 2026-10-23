package com.example.lab2.repository;

import com.example.lab2.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository implements EntityRepository<User> {

    private static final RowMapper<User> USER_MAPPER = (rs, rowNum) ->
            new User(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("email")
            );

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<User> findAll() {
        return jdbc.query("""
                SELECT id, name, email
                FROM public.users
                ORDER BY id
                """, USER_MAPPER);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jdbc.query("""
                SELECT id, name, email
                FROM public.users
                WHERE id = ?
                """, USER_MAPPER, id)
                .stream()
                .findFirst();
    }

    @Override
    public User save(User user) {
        if (user.id() == null) {
            return jdbc.queryForObject("""
                    INSERT INTO public.users (name, email)
                    VALUES (?, ?)
                    RETURNING id, name, email
                    """, USER_MAPPER, user.name(), user.email());
        }

        return jdbc.queryForObject("""
                UPDATE public.users
                SET name = ?, email = ?
                WHERE id = ?
                RETURNING id, name, email
                """, USER_MAPPER, user.name(), user.email(), user.id());
    }

    @Override
    public void deleteById(Long id) {
        jdbc.update(
                "DELETE FROM public.users WHERE id = ?",
                id
        );
    }
}