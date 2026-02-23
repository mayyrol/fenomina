package com.fenomina.auth.service;

import com.fenomina.auth.config.JwtConfig;
import com.fenomina.auth.dto.request.LoginRequestDTO;
import com.fenomina.auth.dto.response.AuthResponseDTO;
import com.fenomina.auth.dto.response.MessageResponseDTO;
import com.fenomina.auth.dto.response.UsuarioResponseDTO;
import com.fenomina.auth.entity.RefreshToken;
import com.fenomina.auth.entity.Usuario;
import com.fenomina.auth.exceptions.AuthException;
import com.fenomina.auth.mappers.UsuarioMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de autenticación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final JwtConfig jwtConfig;

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO loginRequest, String ipAddress, String userAgent) {
        Usuario usuario = usuarioService.obtenerUsuarioPorUsername(loginRequest.getUserName());

        usuarioService.intentarDesbloqueoAutomatico(usuario);

        if (!usuario.estaActivo()) {
            auditLogService.registrarLoginFallido(
                    loginRequest.getUserName(),
                    ipAddress,
                    userAgent,
                    "Usuario bloqueado"
            );
            throw AuthException.usuarioBloqueado();
        }

        if (!passwordEncoder.matches(loginRequest.getContrasenaUsuario(), usuario.getContrasenaUsuario())) {
            // Incrementar intentos fallidos
            usuario.incrementarIntentosLogin();

            // Bloquear si alcanza 5 intentos
            if (usuario.getIntentosFallidosLogin() >= 5) {
                usuario.bloquear();
                auditLogService.registrarBloqueoUsuario(
                        usuario,
                        ipAddress,
                        "Bloqueado automáticamente por 5 intentos fallidos"
                );
            }

            usuarioService.guardarUsuario(usuario);

            auditLogService.registrarLoginFallido(
                    loginRequest.getUserName(),
                    ipAddress,
                    userAgent,
                    "Contraseña incorrecta - Intento " + usuario.getIntentosFallidosLogin()
            );

            throw AuthException.invalidCredentials();
        }

        // Login exitoso - resetear intentos fallidos
        usuario.resetearIntentosLogin();
        usuario.setUltimoLogin(java.time.LocalDateTime.now());

        // Generar tokens
        String accessToken = jwtService.generateToken(usuario);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(usuario, ipAddress, userAgent);

        // Registrar login exitoso en auditoría
        auditLogService.registrarLoginExitoso(usuario, ipAddress, userAgent);

        log.info("Login exitoso - Usuario: {} desde IP: {}", usuario.getUserName(), ipAddress);

        // Construir respuesta
        UsuarioResponseDTO usuarioDTO = usuarioMapper.toResponseDTO(usuario);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtConfig.getExpiration() / 1000) // Convertir a segundos
                .tokenType("Bearer")
                .usuario(usuarioDTO)
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO refreshToken(String refreshTokenStr, String ipAddress) {
        // Validar refresh token
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenStr)
                .orElseThrow(AuthException::tokenInvalido);

        Usuario usuario = refreshToken.getUsuario();

        // Verificar que el usuario siga activo
        if (!usuario.estaActivo()) {
            throw AuthException.usuarioBloqueado();
        }

        refreshTokenService.revokeToken(refreshTokenStr);

        String newAccessToken = jwtService.generateToken(usuario);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(
                usuario,
                ipAddress,
                refreshToken.getUserAgent()
        );

        // Registrar en auditoría
        auditLogService.registrarTokenRefresh(usuario, ipAddress);

        log.info("Token renovado - Usuario: {}", usuario.getUserName());

        UsuarioResponseDTO usuarioDTO = usuarioMapper.toResponseDTO(usuario);

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .expiresIn(jwtConfig.getExpiration() / 1000)
                .tokenType("Bearer")
                .usuario(usuarioDTO)
                .build();
    }

    @Override
    @Transactional
    public MessageResponseDTO logout(String refreshTokenStr, String ipAddress) {
        // Validar y obtener refresh token
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenStr)
                .orElseThrow(AuthException::tokenInvalido);

        Usuario usuario = refreshToken.getUsuario();

        // Revocar todos los tokens del usuario (cierra todas las sesiones)
        refreshTokenService.revokeAllUserTokens(usuario.getUsuarioId());

        // Registrar logout en auditoría
        auditLogService.registrarLogout(usuario, ipAddress);

        log.info("Logout exitoso - Usuario: {} desde IP: {}", usuario.getUserName(), ipAddress);

        return new MessageResponseDTO("Logout exitoso. Todas las sesiones han sido cerradas.");
    }

    @Override
    public boolean validateToken(String token) {
        try {
            String username = jwtService.extractUsername(token);
            Usuario usuario = usuarioService.obtenerUsuarioPorUsername(username);
            return jwtService.isTokenValid(token, usuario);
        } catch (Exception e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }
}
