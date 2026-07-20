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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private final AuthenticationManager authenticationManager;

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private final JwtService jwtService;

    @Autowired
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username já está em uso");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email já está em uso");
        }

        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .role(req.role() != null ? req.role() : Role.USER) // default USER se vier nulo
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }



    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );

        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado"));

        String accessToken = jwtService.generateAccessToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest req) {
        RefreshToken storedToken = refreshTokenService.verifyAndGet(req.refreshToken());
        User user = storedToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(user.getUsername());

        // sem rotation: devolve o mesmo refresh token que ainda é válido
        return new AuthResponse(newAccessToken, storedToken.getToken());
    }

    @PostMapping("/logout")
    public void logout(@RequestBody RefreshRequest req) {
        RefreshToken storedToken = refreshTokenService.verifyAndGet(req.refreshToken());
        refreshTokenService.revokeByUser(storedToken.getUser());
    }
}