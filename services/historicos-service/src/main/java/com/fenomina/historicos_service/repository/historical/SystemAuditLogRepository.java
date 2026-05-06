package com.fenomina.historicos_service.repository.historical;

import com.fenomina.historicos_service.entity.historical.SystemAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, Long> {

    @Query("""
        SELECT s FROM SystemAuditLog s
        WHERE (:usuarioId IS NULL OR s.usuarioId = :usuarioId)
          AND (:username IS NULL OR LOWER(s.username) LIKE LOWER(CONCAT('%', :username, '%')))
          AND (:tablaAfectada IS NULL OR s.tablaAfectada = :tablaAfectada)
          AND (:operacion IS NULL OR s.operacion = :operacion)
          AND (:empresaId IS NULL OR s.fkIdEmpresa = :empresaId)
          AND (:desde IS NULL OR s.timestamp >= :desde)
          AND (:hasta IS NULL OR s.timestamp <= :hasta)
        ORDER BY s.timestamp DESC
        """)
    Page<SystemAuditLog> findByFiltros(
            @Param("usuarioId") Long usuarioId,
            @Param("username") String username,
            @Param("tablaAfectada") String tablaAfectada,
            @Param("operacion") String operacion,
            @Param("empresaId") Long empresaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );
}
