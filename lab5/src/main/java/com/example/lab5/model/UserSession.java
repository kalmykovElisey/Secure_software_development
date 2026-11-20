package com.example.lab5.model;

import java.time.Instant;
import java.util.UUID;

public record UserSession(
        UUID id,
        String username,
        String deviceId,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiry,
        Instant refreshTokenExpiry,
        SessionStatus status
) {
    @Override
    public String toString() {
        return "UserSession[id=" + id + ", status=" + status + "]";
    }
}
