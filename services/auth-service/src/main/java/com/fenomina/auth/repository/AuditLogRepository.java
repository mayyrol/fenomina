package com.fenomina.auth.repository;

import com.fenomina.auth.entity.AuditLog;
import com.fenomina.auth.enums.TipoAccionAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Buscar logs de un usuario específico (paginado)
    Page<AuditLog> findByUsuarioIdOrderByTimestampDesc(Long usuarioId, Pageable pageable);

    // Buscar logs por tipo de acción
    Page<AuditLog> findByAccionOrderByTimestampDesc(TipoAccionAudit accion, Pageable pageable);

    // Buscar logs por usuario y acción
    @Query("SELECT al FROM AuditLog al WHERE al.usuarioId = :usuarioId AND al.accion = :accion ORDER BY al.timestamp DESC")
    List<AuditLog> findByUsuarioAndAccion(@Param("usuarioId") Long usuarioId, @Param("accion") TipoAccionAudit accion);

    // Buscar intentos de login fallidos de un username
    @Query("SELECT al FROM AuditLog al WHERE al.username = :username AND al.accion = 'LOGIN_FAILED' ORDER BY al.timestamp DESC")
    List<AuditLog> findFailedLoginAttempts(@Param("username") String username);

    // Buscar logs en un rango de fechas
    @Query("SELECT al FROM AuditLog al WHERE al.timestamp BETWEEN :desde AND :hasta ORDER BY al.timestamp DESC")
    Page<AuditLog> findByDateRange(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, Pageable pageable);

    // Buscar logs por dirección IP (detectar actividad sospechosa)
    @Query("SELECT al FROM AuditLog al WHERE al.ipAddress = :ip ORDER BY al.timestamp DESC")
    List<AuditLog> findByIpAddress(@Param("ip") String ip);

    // Contar intentos fallidos recientes (últimos 30 minutos)
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.username = :username AND al.accion = 'LOGIN_FAILED' AND al.timestamp > :desde")
    long countRecentFailedAttempts(@Param("username") String username, @Param("desde") LocalDateTime desde);
}
