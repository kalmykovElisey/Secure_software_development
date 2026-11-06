package com.example.lab4.service;

import com.example.lab4.dto.AuthRequests.*;
import com.example.lab4.exception.ApiException;
import com.example.lab4.model.User;
import com.example.lab4.repository.AccountRepository;
import com.example.lab4.security.AccountPrincipal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final RecyclingService recycling = mock(RecyclingService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final AuthService service = new AuthService(accounts, recycling, encoder);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short", "alllowercase123!", "ALLUPPERCASE123!", "NoDigitsHere!",
            "NoSpecialChars123", "HasControl123!\n"})
    void rejectsWeakPasswords(String password) {
        assertThatThrownBy(() -> AuthService.checkPassword(password)).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsPasswordOverBcryptByteLimit() {
        assertThatThrownBy(() -> AuthService.checkPassword("Яя1!" + "я".repeat(40)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void registrationAlwaysCreatesUserAndStoresOnlyHash() {
        when(recycling.createUser(any())).thenReturn(new User(10L, "Анна", "a@example.com"));
        Registration request = new Registration();
        request.name = "Анна"; request.email = "a@example.com"; request.password = "Long-Password!42";
        assertThat(service.register(request).role()).isEqualTo("USER");
        verify(accounts).create(eq(10L), argThat(hash -> !hash.equals(request.password)
                && encoder.matches(request.password, hash)), eq("USER"));
    }

    @Test
    void existingAdminIsNeverRecreatedOrPromotedOnRestart() {
        when(accounts.hasAdmin()).thenReturn(true);
        service.createInitialAdmin("admin@example.com", "Long-Password!42");
        verifyNoInteractions(recycling);
        verify(accounts, never()).create(any(), any(), any());
    }

    @Test
    void cannotChangeOwnRoleOrStatus() {
        assertThatThrownBy(() -> service.update(10L, new AccountUpdate(), 10L))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(accounts);
    }

    @Test
    void adminCanDisableAccount() {
        when(accounts.findByUserId(10L)).thenReturn(Optional.of(
                new AccountPrincipal(10L, "a@example.com", "hash", "USER", true)));
        when(recycling.getUser(10L)).thenReturn(new User(10L, "Анна", "a@example.com"));
        AccountUpdate request = new AccountUpdate(); request.enabled = false;
        assertThat(service.update(10L, request, 1L).enabled()).isFalse();
        verify(accounts).update(10L, "USER", false);
    }
}
