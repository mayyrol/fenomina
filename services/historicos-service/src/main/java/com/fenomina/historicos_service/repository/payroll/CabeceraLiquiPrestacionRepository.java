package com.fenomina.historicos_service.repository.payroll;

import com.fenomina.historicos_service.entity.payroll.CabeceraLiquiPrestacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CabeceraLiquiPrestacionRepository extends JpaRepository<CabeceraLiquiPrestacion, Long> {

    @Query("""
        SELECT c FROM CabeceraLiquiPrestacion c
        JOIN ProcesoLiquidacion pl ON c.fkProcesoLiquiId = pl.procesoLiquiId
        WHERE pl.fkIdEmpresa = :empresaId
          AND (:anio IS NULL OR c.anioLiquiPrestacion = :anio)
          AND (:periodo IS NULL OR c.periodoLiquiPrestacion = :periodo)
          AND pl.estadoProcNomina = 'PAGADO'
        ORDER BY c.anioLiquiPrestacion DESC, c.periodoLiquiPrestacion DESC
        """)
    Page<CabeceraLiquiPrestacion> findByEmpresaYFiltros(
            @Param("empresaId") Long empresaId,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            Pageable pageable
    );
}