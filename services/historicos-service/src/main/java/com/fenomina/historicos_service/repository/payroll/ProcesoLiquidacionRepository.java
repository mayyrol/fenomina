package com.fenomina.historicos_service.repository.payroll;

import com.fenomina.historicos_service.entity.payroll.ProcesoLiquidacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcesoLiquidacionRepository extends JpaRepository<ProcesoLiquidacion, Long> {

    @Query("""
        SELECT p FROM ProcesoLiquidacion p
        WHERE p.fkIdEmpresa = :empresaId
          AND (:tipoProceso IS NULL OR p.tipoProceso = :tipoProceso)
          AND (:estado IS NULL OR p.estadoProcNomina = :estado)
          AND (:anio IS NULL OR p.anio = :anio)
          AND (:periodo IS NULL OR p.periodo = :periodo)
        ORDER BY p.anio DESC, p.periodo DESC
        """)
    Page<ProcesoLiquidacion> findByEmpresaYFiltros(
            @Param("empresaId") Long empresaId,
            @Param("tipoProceso") String tipoProceso,
            @Param("estado") String estado,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            Pageable pageable
    );
}
