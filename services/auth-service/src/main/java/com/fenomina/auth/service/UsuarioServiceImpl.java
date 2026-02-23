package com.fenomina.auth.service;

import com.fenomina.auth.dto.request.ActualizarUsuarioRequestDTO;
import com.fenomina.auth.dto.request.RegistroUsuarioRequestDTO;
import com.fenomina.auth.dto.response.UsuarioResponseDTO;
import com.fenomina.auth.entity.Usuario;
import com.fenomina.auth.exceptions.AuthException;
import com.fenomina.auth.mappers.UsuarioMapper;
import com.fenomina.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de gestión de usuarios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final UsuarioMapper usuarioMapper;

    @Override
    @Transactional
    public UsuarioResponseDTO crearUsuario(RegistroUsuarioRequestDTO dto, String ipAddress) {
        // Validar que no exista el username
        if (existeUsername(dto.getUserName())) {
            throw AuthException.usuarioYaExiste(dto.getUserName());
        }

        // Validar que no exista el número de identificación
        if (existeNumeroIdentificacion(dto.getNumIdentiUsuario())) {
            throw AuthException.numeroIdentificacionYaExiste(dto.getNumIdentiUsuario());
        }

        // Crear entidad Usuario
        Usuario usuario = Usuario.builder()
                .userName(dto.getUserName())
                .nombresUsuario(dto.getNombresUsuario())
                .apellidosUsuario(dto.getApellidosUsuario())
                .numIdentiUsuario(dto.getNumIdentiUsuario())
                .cargoUsuario(dto.getCargoUsuario())
                .contrasenaUsuario(passwordEncoder.encode(dto.getContrasenaUsuario()))
                .rolUsuario(dto.getRolUsuario())
                .fkIdEmpresa(dto.getFkIdEmpresa())
                .estadoUsuario(true)
                .intentosFallidosLogin(0)
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // Obtener usuario creador
        Usuario usuarioCreador = obtenerUsuarioActual();

        // Registrar en auditoría
        auditLogService.registrarCreacionUsuario(usuarioCreador, usuarioGuardado, ipAddress);

        log.info("Usuario creado exitosamente: {}", usuarioGuardado.getUserName());

        return usuarioMapper.toResponseDTO(usuarioGuardado);
    }

    @Override
    public Usuario obtenerUsuarioPorUsername(String username) {
        return usuarioRepository.findByUserNameAndNotDeleted(username)
                .orElseThrow(() -> AuthException.usuarioNoEncontrado(username));
    }

    @Override
    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> AuthException.usuarioNoEncontrado("ID: " + id));
    }

    @Override
    public List<UsuarioResponseDTO> listarUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAllActive();
        return usuarios.stream()
                .map(usuarioMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UsuarioResponseDTO actualizarUsuario(Long id, ActualizarUsuarioRequestDTO dto, String ipAddress) {
        Usuario usuario = obtenerUsuarioPorId(id);

        // Construir string con los cambios realizados
        StringBuilder cambios = new StringBuilder();

        // Actualizar campos si vienen en el DTO
        if (dto.getNombresUsuario() != null && !dto.getNombresUsuario().equals(usuario.getNombresUsuario())) {
            cambios.append("nombres: ").append(usuario.getNombresUsuario())
                    .append(" → ").append(dto.getNombresUsuario()).append("; ");
            usuario.setNombresUsuario(dto.getNombresUsuario());
        }

        if (dto.getApellidosUsuario() != null && !dto.getApellidosUsuario().equals(usuario.getApellidosUsuario())) {
            cambios.append("apellidos: ").append(usuario.getApellidosUsuario())
                    .append(" → ").append(dto.getApellidosUsuario()).append("; ");
            usuario.setApellidosUsuario(dto.getApellidosUsuario());
        }

        if (dto.getCargoUsuario() != null && !dto.getCargoUsuario().equals(usuario.getCargoUsuario())) {
            cambios.append("cargo: ").append(usuario.getCargoUsuario())
                    .append(" → ").append(dto.getCargoUsuario()).append("; ");
            usuario.setCargoUsuario(dto.getCargoUsuario());
        }

        if (dto.getRolUsuario() != null && !dto.getRolUsuario().equals(usuario.getRolUsuario())) {
            cambios.append("rol: ").append(usuario.getRolUsuario())
                    .append(" → ").append(dto.getRolUsuario()).append("; ");
            usuario.setRolUsuario(dto.getRolUsuario());
        }

        if (dto.getFkIdEmpresa() != null && !dto.getFkIdEmpresa().equals(usuario.getFkIdEmpresa())) {
            cambios.append("empresaId: ").append(usuario.getFkIdEmpresa())
                    .append(" → ").append(dto.getFkIdEmpresa()).append("; ");
            usuario.setFkIdEmpresa(dto.getFkIdEmpresa());
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuario);

        // Registrar en auditoría
        Usuario usuarioEditor = obtenerUsuarioActual();
        auditLogService.registrarActualizacionUsuario(
                usuarioEditor,
                usuarioActualizado,
                ipAddress,
                cambios.toString()
        );

        log.info("Usuario actualizado: {} - Cambios: {}", usuario.getUserName(), cambios);

        return usuarioMapper.toResponseDTO(usuarioActualizado);
    }

    @Override
    public UsuarioResponseDTO obtenerUsuarioDTOPorId(Long id) {
        Usuario usuario = obtenerUsuarioPorId(id);
        return usuarioMapper.toResponseDTO(usuario);
    }

    @Transactional
    public void guardarUsuario(Usuario usuario) {
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void eliminarUsuario(Long id, String ipAddress) {
        Usuario usuario = obtenerUsuarioPorId(id);

        // Soft delete
        usuario.setDeletedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // Registrar en auditoría
        Usuario usuarioEliminador = obtenerUsuarioActual();
        auditLogService.registrarEliminacionUsuario(usuarioEliminador, usuario, ipAddress);

        log.info("Usuario eliminado (soft delete): {}", usuario.getUserName());
    }

    @Override
    @Transactional
    public void desbloquearUsuario(Long id, String ipAddress) {
        Usuario usuario = obtenerUsuarioPorId(id);

        if (usuario.estaActivo()) {
            log.warn("Intento de desbloquear usuario que no está bloqueado: {}", usuario.getUserName());
            return; // No hacer nada si ya está activo
        }

        usuario.desbloquear();
        usuarioRepository.save(usuario);

        // Registrar en auditoría
        Usuario usuarioDesbloqueador = obtenerUsuarioActual();
        auditLogService.registrarDesbloqueoUsuario(usuarioDesbloqueador, usuario, ipAddress);

        log.info("Usuario desbloqueado manualmente: {} por {}",
                usuario.getUserName(), usuarioDesbloqueador.getUserName());
    }

    @Override
    public boolean existeUsername(String username) {
        return usuarioRepository.existsByUserName(username);
    }

    @Override
    public boolean existeNumeroIdentificacion(String numIdentificacion) {
        return usuarioRepository.existsByNumIdentiUsuario(numIdentificacion);
    }

    @Override
    @Transactional
    public void intentarDesbloqueoAutomatico(Usuario usuario) {
        if (usuario.puedeDesbloquearseAutomaticamente()) {
            usuario.desbloquear();
            usuarioRepository.save(usuario);
            log.info("Usuario desbloqueado automáticamente después de 15 minutos: {}", usuario.getUserName());
        }
    }

    /**
     * Obtiene el usuario actualmente autenticado
     * Usado para auditoría (saber quién hizo qué).
     */
    private Usuario obtenerUsuarioActual() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return obtenerUsuarioPorUsername(username);
    }
}
