package com.fenomina.auth.repository;

import com.fenomina.auth.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar por username (para login)
    Optional<Usuario> findByUserName(String userName);

    // Buscar por username y que esté activo (no eliminado)
    @Query("SELECT u FROM Usuario u WHERE u.userName = :userName AND u.deletedAt IS NULL")
    Optional<Usuario> findByUserNameAndNotDeleted(@Param("userName") String userName);

    // Buscar por username incluyendo eliminados (para auditoría)
    Optional<Usuario> findByUserNameIgnoreCase(String userName);

    // Verificar si existe username (para validación en registro)
    boolean existsByUserName(String userName);

    // Verificar si existe número de identificación
    boolean existsByNumIdentiUsuario(String numIdenti);

    // Buscar por ID y que esté activo
    @Query("SELECT u FROM Usuario u WHERE u.usuarioId = :id AND u.deletedAt IS NULL")
    Optional<Usuario> findByIdAndNotDeleted(@Param("id") Long id);

    // Listar usuarios activos (no eliminados)
    @Query("SELECT u FROM Usuario u WHERE u.deletedAt IS NULL ORDER BY u.createdAt DESC")
    java.util.List<Usuario> findAllActive();

    // Buscar usuarios por empresa (para futura funcionalidad multi)
    @Query("SELECT u FROM Usuario u WHERE u.fkIdEmpresa = :empresaId AND u.deletedAt IS NULL")
    java.util.List<Usuario> findByEmpresaId(@Param("empresaId") Long empresaId);
}