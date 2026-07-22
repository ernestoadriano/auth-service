package dev.jwt.auth.service;

import dev.jwt.auth.entity.RefreshToken;
import dev.jwt.auth.entity.User;
import dev.jwt.auth.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        String tokenValue = jwtService.generateRefreshToken(user.getUsername());

        Instant now = Instant.now();
        Instant expiryDate = now.plus(jwtService.getRefreshTokenExpirationDays(), ChronoUnit.DAYS);

        log.info("Refresh token criado para: {}", user.getUsername());
        log.info(" Expira em: {}", FORMATTER.format(expiryDate));
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(Instant.now().plus(jwtService.getRefreshTokenExpirationDays(), ChronoUnit.DAYS))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyAndGet(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido"));

        Instant now = Instant.now();
        Instant expiry = refreshToken.getExpiryDate();
        log.info("Refresh token verificado");
        log.info("Expira em: {}", FORMATTER.format(expiry));
        log.info("Status: {}", expiry.isBefore(now) ? "EXPIRADO" : "VÁLIDO");
        if (refreshToken.isRevoked()) {
            throw new BadCredentialsException("Refresh token revogado");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadCredentialsException("Refresh token expirado, por favor faça login novamente");
        }

        return refreshToken;
    }

    public void revokeByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
        log.info("Tokens revogados para: {}", user.getUsername());
    }
}