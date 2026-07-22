package dev.jwt.auth.controller;

import dev.jwt.auth.dto.request.LoginRequest;
import dev.jwt.auth.dto.request.RefreshRequest;
import dev.jwt.auth.dto.request.RegisterRequest;
import dev.jwt.auth.dto.response.AuthResponse;
import dev.jwt.auth.entity.RefreshToken;
import dev.jwt.auth.entity.Role;
import dev.jwt.auth.entity.User;
import dev.jwt.auth.repository.UserRepository;
import dev.jwt.auth.service.JwtService;
import dev.jwt.auth.service.RefreshTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("UTC"));

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );

            User user = userRepository.findByUsername(req.username())
                    .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado"));

            String accessToken = jwtService.generateAccessToken(user.getUsername());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            // LOG DO REFRESH TOKEN
            log.info("Login realizado para: {}", user.getUsername());
            log.info("Refresh token expira em: {}",
                    FORMATTER.format(refreshToken.getExpiryDate()));

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken()));

        } catch (BadCredentialsException e) {
            log.warn("❌ Login falhou para: {}", req.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Credenciais inválidas", null));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest req) {
        try {
            RefreshToken storedToken = refreshTokenService.verifyAndGet(req.refreshToken());
            User user = storedToken.getUser();

            refreshTokenService.revokeByUser(user);
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);
            String newAccessToken = jwtService.generateAccessToken(user.getUsername());

            // LOG DO NOVO REFRESH TOKEN
            log.info("🔄 Refresh realizado para: {}", user.getUsername());
            log.info("   📅 Novo refresh token expira em: {}",
                    FORMATTER.format(newRefreshToken.getExpiryDate()));

            return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken.getToken()));

        } catch (BadCredentialsException e) {
            log.warn("❌ Refresh falhou: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Refresh token inválido ou expirado", null));
        } catch (Exception e) {
            log.error("❌ Erro no refresh: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("Erro ao renovar token", null));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        try {
            if (userRepository.existsByUsername(req.username())) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse("Username já está em uso", null));
            }
            if (userRepository.existsByEmail(req.email())) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse("Email já está em uso", null));
            }

            User user = User.builder()
                    .username(req.username())
                    .email(req.email())
                    .password(passwordEncoder.encode(req.password()))
                    .role(req.role() != null ? req.role() : Role.USER)
                    .build();

            userRepository.save(user);

            String accessToken = jwtService.generateAccessToken(user.getUsername());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            log.info("Registro realizado para: {}", user.getUsername());
            log.info("Refresh token expira em: {}",
                    FORMATTER.format(refreshToken.getExpiryDate()));

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken()));

        } catch (Exception e) {
            log.error("❌ Erro no registro: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("Erro ao registar utilizador", null));
        }
    }

    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
        try {
            RefreshToken storedToken = refreshTokenService.getByToken(request.refreshToken());

            if (storedToken != null) {
                refreshTokenService.deleteToken(storedToken);
                log.info("Logout realizado para: {}", storedToken.getUser().getUsername());
            }
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            log.error("Erro no logout: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}