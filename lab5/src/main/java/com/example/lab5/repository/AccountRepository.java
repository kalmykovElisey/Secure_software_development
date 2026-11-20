package com.example.lab5.repository;

import com.example.lab5.security.AccountPrincipal;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {
    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String SELECT_ACCOUNT = """
            SELECT u.id, u.email, a.password_hash, a.role, a.enabled
            FROM public.user_accounts a JOIN public.users u ON u.id = a.user_id
            """;

    public Optional<AccountPrincipal> findByEmail(String email) {
        return find(SELECT_ACCOUNT + " WHERE lower(btrim(u.email)) = ?", email);
    }

    public Optional<AccountPrincipal> findByUserId(Long userId) {
        return find(SELECT_ACCOUNT + " WHERE u.id = ?", userId);
    }

    private Optional<AccountPrincipal> find(String sql, Object parameter) {
        return jdbc.query(sql, (rs, row) -> new AccountPrincipal(
                rs.getLong("id"), rs.getString("email"), rs.getString("password_hash"),
                rs.getString("role"), rs.getBoolean("enabled")
        ), parameter).stream().findFirst();
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
