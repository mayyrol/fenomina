package com.fenomina.historicos_service.repository.master;

import com.fenomina.historicos_service.entity.master.Empleado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    List<Empleado> findByFkIdEmpresa(Long fkIdEmpresa);

    @Query("""
        SELECT e FROM Empleado e
        WHERE e.fkIdEmpresa = :empresaId
          AND (:documento IS NULL OR e.documentoEmp LIKE CONCAT('%', :documento, '%'))
          AND (:nombres IS NULL OR LOWER(CONCAT(e.nombresEmp, ' ', e.apellidosEmp))
               LIKE LOWER(CONCAT('%', :nombres, '%')))
          AND (:estado IS NULL OR e.estadoEmp = :estado)
        ORDER BY e.apellidosEmp ASC
        """)
    Page<Empleado> findByEmpresaYFiltros(
            @Param("empresaId") Long empresaId,
            @Param("documento") String documento,
            @Param("nombres") String nombres,
            @Param("estado") String estado,
            Pageable pageable
    );
}