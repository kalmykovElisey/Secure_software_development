package com.example.lab5.controller;

import com.example.lab5.dto.AuthRequests.*;
import com.example.lab5.exception.ApiException;
import com.example.lab5.service.TokenService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class TokenController {
    private final TokenService tokens;

    public TokenController(TokenService tokens) {
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPair> login(@RequestBody Login request) {
        return response(HttpStatus.OK, tokens.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<TokenPair> register(@RequestBody Registration request) {
        return response(HttpStatus.CREATED, tokens.register(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestBody Refresh request) {
        TokenPair pair = tokens.refresh(request.refreshToken())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "Refresh-токен или сессия недействительны"));
        return response(HttpStatus.OK, pair);
    }

    private ResponseEntity<TokenPair> response(HttpStatus status, TokenPair pair) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(pair);
    }
}
