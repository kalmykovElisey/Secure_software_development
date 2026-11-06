package com.example.lab4.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public final class AuthRequests {
    private AuthRequests() {}

    public static class Registration {
        @JsonSetter(nulls = Nulls.FAIL)
        public String name;
        @JsonSetter(nulls = Nulls.FAIL)
        public String email;
        @JsonSetter(nulls = Nulls.FAIL)
        public String password;
    }

    public static class AccountUpdate {
        @JsonSetter(nulls = Nulls.FAIL)
        public String role;
        @JsonSetter(nulls = Nulls.FAIL)
        public Boolean enabled;
    }

    public record AccountView(
            Long userId, String name, String email, String role, boolean enabled
    ) {}
}
