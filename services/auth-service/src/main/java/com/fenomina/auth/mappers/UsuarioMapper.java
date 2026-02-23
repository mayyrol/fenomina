package com.fenomina.auth.mappers;

import com.fenomina.auth.dto.response.UsuarioResponseDTO;
import com.fenomina.auth.entity.Usuario;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entidad Usuario a DTOs.
 * No expone información sensible como contraseñas.
 */
@Component
public class UsuarioMapper {

    /**
     * Convierte Usuario (entidad) a UsuarioResponseDTO.
     * Excluye la contraseña por seguridad.
     */
    public UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        return UsuarioResponseDTO.builder()
                .usuarioId(usuario.getUsuarioId())
                .userName(usuario.getUserName())
                .nombresUsuario(usuario.getNombresUsuario())
                .apellidosUsuario(usuario.getApellidosUsuario())
                .numIdentiUsuario(usuario.getNumIdentiUsuario())
                .cargoUsuario(usuario.getCargoUsuario())
                .rolUsuario(usuario.getRolUsuario())
                .fkIdEmpresa(usuario.getFkIdEmpresa())
                .estadoUsuario(usuario.getEstadoUsuario())
                .ultimoLogin(usuario.getUltimoLogin())
                .createdAt(usuario.getCreatedAt())
                .build();
    }
}
