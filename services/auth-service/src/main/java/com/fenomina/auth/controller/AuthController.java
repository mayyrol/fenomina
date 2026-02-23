package com.fenomina.auth.controller;

import com.fenomina.auth.dto.request.LoginRequestDTO;
import com.fenomina.auth.dto.request.RefreshTokenRequestDTO;
import com.fenomina.auth.dto.response.AuthResponseDTO;
import com.fenomina.auth.dto.response.MessageResponseDTO;
import com.fenomina.auth.service.AuthService;
import com.fenomina.auth.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación.
 * Endpoints públicos (no requieren token).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Login - Autenticación con username y contraseña.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginRequest,
            HttpServletRequest request) {

        String ipAddress = IpUtils.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("Intento de login - Username: {} desde IP: {}", loginRequest.getUserName(), ipAddress);

        AuthResponseDTO response = authService.login(loginRequest, ipAddress, userAgent);

        return ResponseEntity.ok(response);
    }

    /**
     * Refresh Token - Renovar access token usando refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequest,
            HttpServletRequest request) {

        String ipAddress = IpUtils.getClientIp(request);

        log.debug("Renovación de token desde IP: {}", ipAddress);

        AuthResponseDTO response = authService.refreshToken(
                refreshTokenRequest.getRefreshToken(),
                ipAddress
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Logout - Cierra sesión del usuario.
     * Revoca todos los refresh tokens.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponseDTO> logout(
            @Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequest,
            HttpServletRequest request) {

        String ipAddress = IpUtils.getClientIp(request);

        log.info("Logout desde IP: {}", ipAddress);

        MessageResponseDTO response = authService.logout(
                refreshTokenRequest.getRefreshToken(),
                ipAddress
        );

        return ResponseEntity.ok(response);
    }
}
