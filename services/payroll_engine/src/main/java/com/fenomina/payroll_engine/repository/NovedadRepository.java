package com.fenomina.payroll_engine.repository;

import com.fenomina.payroll_engine.entity.Novedad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface NovedadRepository extends JpaRepository<Novedad, Long> {

    // Todas las novedades de un empleado en un proceso específico
    List<Novedad> findByFkEmpleadoIdAndProcesoLiquid(Long fkEmpleadoId, Long procesoLiquid);

    // Todas las novedades de un proceso, usado por el motor al liquidar
    List<Novedad> findByProcesoLiquid(Long procesoLiquid);

    // Novedades de un empleado en un periodo, usado para validar duplicados y el límite del 40%
    List<Novedad> findByFkEmpleadoIdAndAnioAndPeriodo(Long fkEmpleadoId, Integer anio, Integer periodo);

    // Novedades sin proceso asignado aún (pendientes de asociar a un proceso)
    @Query("""
            SELECT n FROM Novedad n
            WHERE n.fkEmpleadoId = :empleadoId
            AND n.anio = :anio
            AND n.periodo = :periodo
            AND n.procesoLiquid IS NULL
            """)
    List<Novedad> findNovedadesPendientes(
            @Param("empleadoId") Long empleadoId,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo
    );

    // Verifica si ya existe una novedad del mismo concepto para el empleado en el periodo
    @Query("""
        SELECT COUNT(n) > 0 FROM Novedad n
        LEFT JOIN ProcesoLiquidacion p ON p.procesoLiquiId = n.procesoLiquid
        WHERE n.fkEmpleadoId = :fkEmpleadoId
        AND n.fkConcepNominaId = :fkConcepNominaId
        AND n.anio = :anio
        AND n.periodo = :periodo
        AND (p IS NULL 
             OR p.estadoProcNomina != com.fenomina.payroll_engine.enums.EstadoProceso.ANULADO)
        """)
    boolean existsNovedadActivaByEmpleadoAndConceptoAndPeriodo(
            @Param("fkEmpleadoId") Long fkEmpleadoId,
            @Param("fkConcepNominaId") Long fkConcepNominaId,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo
    );

}
