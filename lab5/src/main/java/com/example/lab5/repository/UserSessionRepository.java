package com.example.lab5.repository;

import com.example.lab5.model.SessionStatus;
import com.example.lab5.model.UserSession;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserSessionRepository {
    private final JdbcTemplate jdbc;

    public UserSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserSession> findByRefreshTokenForUpdate(String token) {
        return jdbc.query("""
                SELECT id, username, device_id, access_token, refresh_token,
                       access_token_expiry, refresh_token_expiry, status
                FROM public.user_sessions WHERE refresh_token = ? FOR UPDATE
                """, (rs, row) -> new UserSession(
                rs.getObject("id", UUID.class), rs.getString("username"),
                rs.getString("device_id"), rs.getString("access_token"), rs.getString("refresh_token"),
                rs.getObject("access_token_expiry", OffsetDateTime.class).toInstant(),
                rs.getObject("refresh_token_expiry", OffsetDateTime.class).toInstant(),
                SessionStatus.valueOf(rs.getString("status"))
        ), token).stream().findFirst();
    }

    public void insert(UserSession session) {
        jdbc.update("""
                INSERT INTO public.user_sessions
                    (id, username, device_id, access_token, refresh_token,
                     access_token_expiry, refresh_token_expiry, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, session.id(), session.username(), session.deviceId(),
                session.accessToken(), session.refreshToken(),
                session.accessTokenExpiry().atOffset(ZoneOffset.UTC),
                session.refreshTokenExpiry().atOffset(ZoneOffset.UTC), session.status().name());
    }

    public void setStatus(UUID id, SessionStatus status) {
        int updated = jdbc.update("UPDATE public.user_sessions SET status = ? WHERE id = ?",
                status.name(), id);
        if (updated != 1) {
            throw new IllegalStateException("Не удалось изменить статус сессии");
        }
    }
}
