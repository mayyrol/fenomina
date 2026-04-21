package com.fenomina.payroll_engine.repository;

import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.enums.EstadoProceso;
import com.fenomina.payroll_engine.enums.TipoProceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcesoLiquidacionRepository extends JpaRepository<ProcesoLiquidacion, Long> {

    @Query("""
            SELECT COUNT(p) > 0 FROM ProcesoLiquidacion p
            WHERE p.fkIdEmpresa = :empresaId
            AND p.tipoProceso = :tipoProceso
            AND p.anio = :anio
            AND p.periodo = :periodo
            AND p.estadoProcNomina IN :estadosActivos
            """)
    boolean existsProcesoActivo(
            @Param("empresaId") Long empresaId,
            @Param("tipoProceso") TipoProceso tipoProceso,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            @Param("estadosActivos") List<EstadoProceso> estadosActivos
    );

    List<ProcesoLiquidacion> findByFkIdEmpresaOrderByAnioDescPeriodoDesc(Long fkIdEmpresa);

    List<ProcesoLiquidacion> findByFkIdEmpresaAndEstadoProcNomina(
            Long fkIdEmpresa,
            EstadoProceso estado
    );
}
