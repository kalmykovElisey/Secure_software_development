package com.example.lab4.repository;

import com.example.lab4.security.AccountPrincipal;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {
    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<AccountPrincipal> findByEmail(String email) {
        return jdbc.query("""
                SELECT u.id, u.email, a.password_hash, a.role, a.enabled
                FROM public.user_accounts a JOIN public.users u ON u.id = a.user_id
                WHERE lower(btrim(u.email)) = ?
                """, (rs, row) -> new AccountPrincipal(
                rs.getLong("id"), rs.getString("email"), rs.getString("password_hash"),
                rs.getString("role"), rs.getBoolean("enabled")
        ), email).stream().findFirst();
    }

    public Optional<AccountPrincipal> findByUserId(Long userId) {
        return jdbc.query("SELECT email FROM public.users WHERE id = ?",
                (rs, row) -> rs.getString("email"), userId)
                .stream().findFirst().flatMap(this::findByEmail);
    }

    public void create(Long userId, String hash, String role) {
        jdbc.update("""
                INSERT INTO public.user_accounts (user_id, password_hash, role, enabled)
                VALUES (?, ?, ?, TRUE)
                """, userId, hash, role);
    }

    public void update(Long userId, String role, boolean enabled) {
        jdbc.update("UPDATE public.user_accounts SET role = ?, enabled = ? WHERE user_id = ?",
                role, enabled, userId);
    }

    public boolean hasAdmin() {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM public.user_accounts WHERE role = 'ADMIN')",
                Boolean.class));
    }
}
