package com.example.lab5.security;

import com.example.lab5.repository.AccountRepository;
import com.example.lab5.service.AuthService;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager passwordAuthentication(AuthService auth, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(auth);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain publicAuth(HttpSecurity http, SecurityErrors errors) throws Exception {
        stateless(http, errors);
        return http.securityMatcher("/auth/login", "/auth/register", "/auth/refresh")
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register", "/auth/refresh")
                        .permitAll()
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokens tokens,
                                                  AccountRepository accounts,
                                                  SecurityErrors errors) throws Exception {
        stateless(http, errors);
        return http.oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(errors).accessDeniedHandler(errors)
                        .jwt(jwt -> jwt.decoder(tokens::readAccess).jwtAuthenticationConverter(claims -> {
                            AccountPrincipal account;
                            try {
                                account = accounts.findByUserId(tokens.userId(claims))
                                        .filter(AccountPrincipal::enabled)
                                        .filter(candidate -> candidate.email().equals(claims.getSubject()))
                                        .orElseThrow(() -> new InvalidBearerTokenException("Недействительный access"));
                            } catch (DataAccessException error) {
                                throw new AuthenticationServiceException("Вход временно недоступен");
                            }
                            var principal = new AccountPrincipal(account.userId(), account.email(), null,
                                    account.role(), account.enabled());
                            return UsernamePasswordAuthenticationToken.authenticated(
                                    principal, null, principal.getAuthorities());
                        })))
                .authorizeHttpRequests(rules -> rules
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .build();
    }

    private void stateless(HttpSecurity http, SecurityErrors errors) {
        // Вход в API возможен только по Bearer; браузер не подставляет его автоматически.
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errors).accessDeniedHandler(errors));
    }

    @Bean
    public SmartInitializingSingleton initialAdmin(AuthService auth,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password:}") String password) {
        return () -> auth.createInitialAdmin(email, password);
    }
}
