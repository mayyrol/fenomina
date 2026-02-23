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

    /**
     * Crea un nuevo usuario en el sistema.
     * Solo puede ser ejecutado por un SUPER_ADMIN.
     *
     * @param dto Datos del usuario a crear
     * @param ipAddress IP desde donde se crea
     * @return UsuarioResponseDTO con los datos del usuario creado
     */
    UsuarioResponseDTO crearUsuario(RegistroUsuarioRequestDTO dto, String ipAddress);

    Usuario obtenerUsuarioPorUsername(String username);

    Usuario obtenerUsuarioPorId(Long id);

    UsuarioResponseDTO obtenerUsuarioDTOPorId(Long id);

    List<UsuarioResponseDTO> listarUsuarios();

    UsuarioResponseDTO actualizarUsuario(Long id, ActualizarUsuarioRequestDTO dto, String ipAddress);

    void eliminarUsuario(Long id, String ipAddress);

    void desbloquearUsuario(Long id, String ipAddress);

    boolean existeUsername(String username);

    boolean existeNumeroIdentificacion(String numIdentificacion);

    void intentarDesbloqueoAutomatico(Usuario usuario);

    void guardarUsuario(Usuario usuario);
}
