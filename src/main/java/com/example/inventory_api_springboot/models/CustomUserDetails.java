package com.example.inventory_api_springboot.models;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {
    private UUID userId;
    private String password;

    public CustomUserDetails(
        UUID userId,
        String password
    ) {
        this.userId = userId;
        this.password = password;
    }

    @Override
    public String getUsername() {
        return this.userId.toString();
    }

    @Override
    public @Nullable String getPassword() {
        return this.password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}
