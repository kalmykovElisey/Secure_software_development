package com.example.lab5.service;

import com.example.lab5.dto.AuthRequests.Login;
import com.example.lab5.dto.AuthRequests.Registration;
import com.example.lab5.dto.AuthRequests.TokenPair;
import com.example.lab5.exception.ApiException;
import com.example.lab5.model.SessionStatus;
import com.example.lab5.model.UserSession;
import com.example.lab5.repository.AccountRepository;
import com.example.lab5.repository.UserSessionRepository;
import com.example.lab5.security.AccountPrincipal;
import com.example.lab5.security.JwtTokens;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {
    private final AuthService auth;
    private final AuthenticationManager authentication;
    private final AccountRepository accounts;
    private final UserSessionRepository sessions;
    private final JwtTokens jwt;
    private final Clock clock;

    public TokenService(AuthService auth, AuthenticationManager authentication, AccountRepository accounts,
                        UserSessionRepository sessions, JwtTokens jwt, Clock clock) {
        this.auth = auth;
        this.authentication = authentication;
        this.accounts = accounts;
        this.sessions = sessions;
        this.jwt = jwt;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TokenPair register(Registration request) {
        String deviceId = deviceId(request.deviceId);
        var account = auth.register(request);
        return issue(account.email(), account.userId(), deviceId);
    }

    @Transactional
    public TokenPair login(Login request) {
        String username = required(request.username(), "username");
        String password = required(request.password(), "password");
        String deviceId = deviceId(request.deviceId());
        if (username.length() > 254 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw badCredentials();
        }
        AccountPrincipal account;
        try {
            var result = authentication.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
            account = (AccountPrincipal) result.getPrincipal();
        } catch (AuthenticationServiceException error) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Вход временно недоступен");
        } catch (AuthenticationException error) {
            throw badCredentials();
        }
        return issue(account.email(), account.userId(), deviceId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Optional<TokenPair> refresh(String token) {
        required(token, "refreshToken");
        Jwt claims;
        try {
            claims = jwt.readRefresh(token);
        } catch (JwtException error) {
            return Optional.empty();
        }

        UserSession old = sessions.findByRefreshTokenForUpdate(token).orElse(null);
        if (old == null || !old.username().equals(claims.getSubject())
                || !old.refreshTokenExpiry().equals(claims.getExpiresAt())
                || !old.refreshTokenExpiry().isAfter(clock.instant())) {
            return Optional.empty();
        }

        if (old.status() == SessionStatus.USED) {
            sessions.setStatus(old.id(), SessionStatus.REVOKED);
            // В контроллере пустой результат станет 401 уже после фиксации транзакции.
            return Optional.empty();
        }
        if (old.status() != SessionStatus.ACTIVE) {
            return Optional.empty();
        }

        boolean active = accounts.findByUserId(jwt.userId(claims))
                .filter(AccountPrincipal::enabled)
                .filter(account -> account.email().equals(old.username()))
                .isPresent();
        if (!active) {
            return Optional.empty();
        }

        sessions.setStatus(old.id(), SessionStatus.USED);
        return Optional.of(issue(old.username(), jwt.userId(claims), old.deviceId()));
    }

    private TokenPair issue(String username, long userId, String deviceId) {
        Instant now = clock.instant();
        Jwt access = jwt.issueAccess(username, userId, now);
        Jwt refresh = jwt.issueRefresh(username, userId, now);
        sessions.insert(new UserSession(UUID.randomUUID(), username, deviceId,
                access.getTokenValue(), refresh.getTokenValue(),
                access.getExpiresAt(), refresh.getExpiresAt(), SessionStatus.ACTIVE));
        return new TokenPair(access.getTokenValue(), refresh.getTokenValue());
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(field + " обязательно");
        }
        return value;
    }

    private static String deviceId(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank() || value.length() > 200) {
            throw ApiException.badRequest("deviceId должен содержать от 1 до 200 символов");
        }
        return value;
    }

    private static ApiException badCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Неверные учётные данные");
    }
}
