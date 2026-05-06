package com.fenomina.payroll_engine.repository;

import com.fenomina.payroll_engine.entity.DetalleLiquiPrestacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleLiquiPrestacionRepository extends JpaRepository<DetalleLiquiPrestacion, Long> {

    // Todos los detalles de una cabecera de prestación
    List<DetalleLiquiPrestacion> findByFkCabeLiquiPrestacionId(Long fkCabeLiquiPrestacionId);

    // Detalles de un empleado en una cabecera específica
    List<DetalleLiquiPrestacion> findByFkCabeLiquiPrestacionIdAndFkEmpleadoId(
            Long fkCabeLiquiPrestacionId,
            Long fkEmpleadoId
    );

    // Historial de prestaciones de un empleado, usado para calcular promedios en prima y cesantías
    @Query("""
        SELECT d FROM DetalleLiquiPrestacion d
        WHERE d.fkEmpleadoId = :empleadoId
        AND d.fkCabeLiquiPrestacionId IN (
            SELECT c.cabeLiquiPrestacionId FROM CabeceraLiquiPrestacion c
            WHERE c.fkProcesoLiquiId IN (
                SELECT p.procesoLiquiId FROM ProcesoLiquidacion p
                WHERE p.estadoProcNomina IN ('PAGADO', 'PENDIENTE_PAGO')
                AND p.anio = :anio
            )
        )
        ORDER BY d.fechaInicioCorteEmp ASC
        """)
    List<DetalleLiquiPrestacion> findHistoricoByEmpleadoAndAnio(
            @Param("empleadoId") Long empleadoId,
            @Param("anio") Integer anio
    );
}
