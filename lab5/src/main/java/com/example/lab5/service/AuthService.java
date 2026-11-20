package com.example.lab5.service;

import com.example.lab5.dto.AuthRequests.*;
import com.example.lab5.dto.Requests.UserRequest;
import com.example.lab5.exception.ApiException;
import com.example.lab5.model.User;
import com.example.lab5.repository.AccountRepository;
import com.example.lab5.security.AccountPrincipal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements UserDetailsService {
    private final AccountRepository accounts;
    private final RecyclingService recycling;
    private final PasswordEncoder encoder;

    public AuthService(AccountRepository accounts, RecyclingService recycling,
                       PasswordEncoder encoder) {
        this.accounts = accounts;
        this.recycling = recycling;
        this.encoder = encoder;
    }

    @Override
    public AccountPrincipal loadUserByUsername(String username) {
        return accounts.findByEmail(username.strip().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Учётная запись не найдена"));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AccountView register(Registration request) {
        return create(request, "USER");
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createInitialAdmin(String email, String password) {
        if (email.isBlank() && password.isBlank()) { return; }
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Задайте ADMIN_EMAIL и ADMIN_PASSWORD вместе");
        }
        if (accounts.hasAdmin()) { return; }
        Registration request = new Registration();
        request.name = "Администратор";
        request.email = email;
        request.password = password;
        create(request, "ADMIN");
    }

    public AccountView me(AccountPrincipal principal) {
        User user = recycling.getUser(principal.userId());
        return new AccountView(user.id(), user.name(), user.email(),
                principal.role(), principal.enabled());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AccountView update(Long userId, AccountUpdate request, Long actingUserId) {
        if (userId.equals(actingUserId)) {
            throw ApiException.conflict("Собственную роль и состояние менять нельзя");
        }
        if (request.role == null && request.enabled == null) {
            throw ApiException.badRequest("Укажите role или enabled");
        }
        AccountPrincipal old = accounts.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Учётная запись не найдена"));
        String role = request.role == null ? old.role() : request.role;
        if (!Set.of("USER", "OPERATOR", "ADMIN").contains(role)) {
            throw ApiException.badRequest("role: допустимы USER, OPERATOR, ADMIN");
        }
        boolean enabled = request.enabled == null ? old.enabled() : request.enabled;
        accounts.update(userId, role, enabled);
        User user = recycling.getUser(userId);
        return new AccountView(user.id(), user.name(), user.email(), role, enabled);
    }

    private AccountView create(Registration request, String role) {
        checkPassword(request.password);
        UserRequest profile = new UserRequest();
        profile.name = request.name;
        profile.email = request.email;
        User user = recycling.createUser(profile);
        accounts.create(user.id(), encoder.encode(request.password), role);
        return new AccountView(user.id(), user.name(), user.email(), role, true);
    }

    static void checkPassword(String password) {
        if (password == null || password.codePointCount(0, password.length()) < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || password.codePoints().anyMatch(Character::isISOControl)
                || !password.matches("(?s).*\\p{Lu}.*")
                || !password.matches("(?s).*\\p{Ll}.*")
                || !password.matches("(?s).*\\p{Nd}.*")
                || !password.matches("(?s).*[^\\p{L}\\p{N}\\s].*")) {
            throw ApiException.badRequest("Пароль: от 12 символов, максимум 72 байта UTF-8, "
                    + "заглавная и строчная буквы, цифра и специальный символ");
        }
    }
}
