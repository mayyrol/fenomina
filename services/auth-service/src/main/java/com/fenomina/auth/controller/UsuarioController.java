package com.fenomina.auth.controller;

import com.fenomina.auth.dto.request.ActualizarUsuarioRequestDTO;
import com.fenomina.auth.dto.request.RegistroUsuarioRequestDTO;
import com.fenomina.auth.dto.response.MessageResponseDTO;
import com.fenomina.auth.dto.response.UsuarioResponseDTO;
import com.fenomina.auth.service.UsuarioService;
import com.fenomina.auth.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador de gestión de usuarios.
 * Todos los endpoints requieren rol SUPER_ADMIN.
 */
@RestController
@RequestMapping("/auth/usuarios")
@RequiredArgsConstructor
@Slf4j
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Crear nuevo usuario.
     * Solo SUPER_ADMIN puede crear usuarios.
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UsuarioResponseDTO> crearUsuario(
            @Valid @RequestBody RegistroUsuarioRequestDTO registroDTO,
            HttpServletRequest request) {

        String ipAddress = IpUtils.getClientIp(request);

        log.info("Creando nuevo usuario: {} desde IP: {}", registroDTO.getUserName(), ipAddress);

        UsuarioResponseDTO response = usuarioService.crearUsuario(registroDTO, ipAddress);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Listar todos los usuarios activos.
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {

        log.debug("Listando todos los usuarios");

        List<UsuarioResponseDTO> usuarios = usuarioService.listarUsuarios();

        return ResponseEntity.ok(usuarios);
    }

    /**
     * Obtener un usuario por ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuario(@PathVariable Long id) {
        log.debug("Obteniendo usuario ID: {}", id);
        return ResponseEntity.ok(usuarioService.obtenerUsuarioDTOPorId(id));
    }

    /**
     * Actualizar un usuario existente.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UsuarioResponseDTO> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequestDTO actualizarDTO,
            HttpServletRequest request) {

        String ipAddress = IpUtils.getClientIp(request);

        log.info("Actualizando usuario ID: {} desde IP: {}", id, ipAddress);

        UsuarioResponseDTO response = usuarioService.actualizarUsuario(id, actualizarDTO, ipAddress);

        return ResponseEntity.ok(response);
    }

    /**
     * Eliminar un usuario (soft delete).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MessageResponseDTO> eliminarUsuario(
            @PathVariable Long id,
            HttpServletRequest request) {

        String ipAddress = IpUtils.getClientIp(request);

        log.info("Eliminando usuario ID: {} desde IP: {}", id, ipAddress);

        usuarioService.eliminarUsuario(id, ipAddress);

        return ResponseEntity.ok(new MessageResponseDTO("Usuario eliminado exitosamente"));
    }

    /**
     * Desbloquear un usuario manualmente.
     */
    @PatchMapping("/{id}/desbloquear")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MessageResponseDTO> desbloquearUsuario(
            @PathVariable Long id,
            HttpServletRequest request) {

        String ipAddress = IpUtils.getClientIp(request);

        log.info("Desbloqueando usuario ID: {} desde IP: {}", id, ipAddress);

        usuarioService.desbloquearUsuario(id, ipAddress);

        return ResponseEntity.ok(new MessageResponseDTO("Usuario desbloqueado exitosamente"));
    }
}