package com.fenomina.master_data_service.repository;

import com.fenomina.master_data_service.entity.EmpresaCorreo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpresaCorreoRepository extends JpaRepository<EmpresaCorreo, Long> {

    @Query("SELECT ec FROM EmpresaCorreo ec WHERE ec.empresaId = :empresaId AND ec.deletedAt IS NULL")
    List<EmpresaCorreo> findByEmpresaIdActive(@Param("empresaId") Long empresaId);
}
