package com.fenomina.auth.service;


import com.fenomina.auth.dto.request.LoginRequestDTO;
import com.fenomina.auth.dto.response.AuthResponseDTO;
import com.fenomina.auth.dto.response.MessageResponseDTO;

/**
 * Servicio principal de autenticación.
 */
public interface AuthService {

    AuthResponseDTO login(LoginRequestDTO loginRequest, String ipAddress, String userAgent);

    AuthResponseDTO refreshToken(String refreshToken, String ipAddress);

    MessageResponseDTO logout(String refreshToken, String ipAddress);

    boolean validateToken(String token);
}