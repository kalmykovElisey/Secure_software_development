package com.example.lab4.security;

import com.example.lab4.service.AuthService;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthService auth,
                                                  PasswordEncoder encoder,
                                                  SecurityErrors errors) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(auth);
        provider.setPasswordEncoder(encoder);
        http.authenticationProvider(provider)
                .csrf(Customizer.withDefaults())
                .httpBasic(basic -> basic.authenticationEntryPoint(errors))
                .securityContext(context -> context
                        .securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .logout(logout -> logout.disable())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .authorizeHttpRequests(rules -> rules
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    public SmartInitializingSingleton initialAdmin(AuthService auth,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password:}") String password) {
        return () -> auth.createInitialAdmin(email, password);
    }
}
