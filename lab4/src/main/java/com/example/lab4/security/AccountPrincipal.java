package com.example.lab4.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AccountPrincipal(
        Long userId, String email, @JsonIgnore String passwordHash,
        String role, boolean enabled
) implements UserDetails {
    @Override
    public String getUsername() { return email; }

    @Override
    @JsonIgnore
    public String getPassword() { return passwordHash; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
