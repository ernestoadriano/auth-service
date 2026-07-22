package dev.jwt.auth.controller;

import dev.jwt.auth.entity.RefreshToken;
import dev.jwt.auth.repository.RefreshTokenRepository;
import dev.jwt.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class TokenExpiryController {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("UTC"));

    @PostMapping("/check-expiry")
    public ResponseEntity<Map<String, String>> checkTokenExpiry(@RequestBody Map<String, String> request) {
        String token = request.get("refreshToken");
        Map<String, String> response = new HashMap<>();

        try {
            RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                    .orElseThrow(() -> new RuntimeException("Token não encontrado"));

            Instant expiry = refreshToken.getExpiryDate();
            Instant now = Instant.now();

            response.put("expires_at", FORMATTER.format(expiry));
            response.put("current_time", FORMATTER.format(now));
            response.put("status", expiry.isBefore(now) ? "EXPIRED" : "VALID");

            log.info("📅 Token expira em: {}", FORMATTER.format(expiry));
            log.info("⏰ Hora atual: {}", FORMATTER.format(now));

        } catch (Exception e) {
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}