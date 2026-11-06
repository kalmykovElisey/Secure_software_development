package com.example.lab4.security;

import com.example.lab4.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrors implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper;
    private final Clock clock;

    public SecurityErrors(ObjectMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        if (exception instanceof AuthenticationServiceException) {
            write(request, response, HttpStatus.SERVICE_UNAVAILABLE, "Вход временно недоступен");
        } else {
            response.setHeader("WWW-Authenticate", "Basic realm=\"recycling\", charset=\"UTF-8\"");
            write(request, response, HttpStatus.UNAUTHORIZED, "Требуется действующая учётная запись");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, exception instanceof CsrfException
                ? "CSRF-токен отсутствует или неверен. Получите токен через /api/auth/csrf"
                : "Недостаточно прав для этой операции");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), new ApiError(clock.instant(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
