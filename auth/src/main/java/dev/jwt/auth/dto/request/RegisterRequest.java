package dev.jwt.auth.dto.request;

import dev.jwt.auth.entity.Role;

public record RegisterRequest(String username, String email, String password, Role role) {
}
