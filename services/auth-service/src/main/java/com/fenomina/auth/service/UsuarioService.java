package com.fenomina.auth.service;

import com.fenomina.auth.dto.request.RegistroUsuarioRequestDTO;
import com.fenomina.auth.dto.request.ActualizarUsuarioRequestDTO;
import com.fenomina.auth.dto.response.UsuarioResponseDTO;
import com.fenomina.auth.entity.Usuario;

import java.util.List;

/**
 * Servicio para gestión de usuarios del sistema.
 * Solo accesible por usuarios con rol SUPER_ADMIN.
 */
public interface UsuarioService {

    UsuarioResponseDTO crearUsuario(RegistroUsuarioRequestDTO dto, String ipAddress);

    Usuario obtenerUsuarioPorUsername(String username);

    Usuario obtenerUsuarioPorId(Long id);

    UsuarioResponseDTO obtenerUsuarioDTOPorId(Long id);

    List<UsuarioResponseDTO> listarUsuarios();

    UsuarioResponseDTO actualizarUsuario(Long id, ActualizarUsuarioRequestDTO dto, String ipAddress);

    void eliminarUsuario(Long id, String ipAddress);

    boolean existeUsername(String username);

    boolean existeNumeroIdentificacion(String numIdentificacion);

    void guardarUsuario(Usuario usuario);

    void activarUsuario(Long id, String ipAddress);

    void inactivarUsuario(Long id, String ipAddress);

    void desbloquearLoginUsuario(Long id, String ipAddress);
}
