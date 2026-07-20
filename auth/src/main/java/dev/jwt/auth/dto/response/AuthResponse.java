package dev.jwt.auth.dto.response;

public record AuthResponse(String accessToken, String refreshToken) {
}
