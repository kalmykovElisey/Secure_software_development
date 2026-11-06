package com.example.lab4.controller;

import com.example.lab4.dto.AuthRequests.*;
import com.example.lab4.security.AccountPrincipal;
import com.example.lab4.service.AuthService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) { this.service = service; }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) { return token; }

    @PostMapping("/register")
    public ResponseEntity<AccountView> register(@RequestBody Registration request) {
        AccountView created = service.register(request);
        return ResponseEntity.created(URI.create("/api/users/" + created.userId())).body(created);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public AccountView me(@AuthenticationPrincipal AccountPrincipal principal) {
        return service.me(principal);
    }

    @PatchMapping("/accounts/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountView update(@PathVariable Long userId, @RequestBody AccountUpdate request,
                              @AuthenticationPrincipal AccountPrincipal principal) {
        return service.update(userId, request, principal.userId());
    }
}
