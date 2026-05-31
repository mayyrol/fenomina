package com.fenomina.payroll_engine.repository;

import com.fenomina.payroll_engine.entity.ReporteNominaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ReporteNominaDetalleRepository extends JpaRepository<ReporteNominaDetalle, Long> {

    // Todos los conceptos calculados para una cabecera, usado en desprendibles
    List<ReporteNominaDetalle> findByFkCabecNominaId(Long fkCabecNominaId);

    // Conceptos de un tipo específico en una cabecera, usado por el motor para verificar
    // si ya fue calculado un concepto antes de persistir
    boolean existsByFkCabecNominaIdAndFkConcepNominaId(Long fkCabecNominaId, Long fkConcepNominaId);

    @Query("""
    SELECT COALESCE(SUM(rnd.cantidadConcept), 0)
    FROM ReporteNominaDetalle rnd
    JOIN NominaCabecera nc ON nc.cabecNominaId = rnd.fkCabecNominaId
    JOIN ProcesoLiquidacion pl ON pl.procesoLiquiId = nc.fkProcesoLiquiId
    WHERE nc.fkEmpleadoId = :empleadoId
    AND pl.estadoProcNomina IN (
        com.fenomina.payroll_engine.enums.EstadoProceso.PAGADO,
        com.fenomina.payroll_engine.enums.EstadoProceso.CERRADO,
        com.fenomina.payroll_engine.enums.EstadoProceso.PENDIENTE_PAGO
    )
    AND rnd.fkConcepNominaId = :concepNominaId
    AND (pl.anio < :anio OR (pl.anio = :anio AND pl.periodo < :periodo))
    """)
    BigDecimal sumDiasIncapacidadAcumulados(
            @Param("empleadoId") Long empleadoId,
            @Param("concepNominaId") Long concepNominaId,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo
    );
}