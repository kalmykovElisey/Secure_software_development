package com.example.lab5.controller;

import com.example.lab5.dto.AuthRequests.*;
import com.example.lab5.security.AccountPrincipal;
import com.example.lab5.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) { this.service = service; }

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
