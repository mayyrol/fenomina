package com.fenomina.historicos_service.repository.auth;

import com.fenomina.historicos_service.entity.auth.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:usuarioId IS NULL OR a.usuarioId = :usuarioId)
          AND (:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%')))
          AND (:accion IS NULL OR a.accion = :accion)
          AND (:desde IS NULL OR a.timestamp >= :desde)
          AND (:hasta IS NULL OR a.timestamp <= :hasta)
        ORDER BY a.timestamp DESC
        """)
    Page<AuditLog> findByFiltros(
            @Param("usuarioId") Long usuarioId,
            @Param("username") String username,
            @Param("accion") String accion,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );
}
