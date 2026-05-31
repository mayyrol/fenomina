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


}
