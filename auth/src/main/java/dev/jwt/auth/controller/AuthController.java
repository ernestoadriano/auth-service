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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

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

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken()));
        } catch (Exception e) {
            log.error("Erro no registo: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("Erro ao registar utilizador", null));
        }
    }

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

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken()));
        } catch (BadCredentialsException e) {
            log.warn("Tentativa de login falhada para: {}", req.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Credenciais inválidas", null));
        } catch (Exception e) {
            log.error("Erro no login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("Erro ao fazer login", null));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest req) {
        try {
            log.debug("Tentativa de refresh token");

            // Verifica e obtém o refresh token
            RefreshToken storedToken = refreshTokenService.verifyAndGet(req.refreshToken());
            User user = storedToken.getUser();

            // Revoga o refresh token antigo
            refreshTokenService.revokeByUser(user);

            // Cria um novo refresh token (rotação)
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

            // Gera um novo access token
            String newAccessToken = jwtService.generateAccessToken(user.getUsername());

            log.debug("Refresh token realizado com sucesso para: {}", user.getUsername());

            return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken.getToken()));
        } catch (BadCredentialsException e) {
            log.warn("Refresh token inválido: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Refresh token inválido ou expirado", null));
        } catch (Exception e) {
            log.error("Erro no refresh token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("Erro ao renovar token", null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest req) {
        try {
            RefreshToken storedToken = refreshTokenService.verifyAndGet(req.refreshToken());
            refreshTokenService.revokeByUser(storedToken.getUser());
            log.debug("Logout realizado com sucesso para: {}", storedToken.getUser().getUsername());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Erro no logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}