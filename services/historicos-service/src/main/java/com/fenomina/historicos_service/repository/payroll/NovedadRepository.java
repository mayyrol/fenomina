package com.fenomina.historicos_service.repository.payroll;

import com.fenomina.historicos_service.entity.payroll.Novedad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NovedadRepository extends JpaRepository<Novedad, Long> {

    List<Novedad> findByFkEmpleadoIdAndAnioAndPeriodo(Long fkEmpleadoId, Integer anio, Integer periodo);

    @Query("""
        SELECT n FROM Novedad n
        WHERE n.fkEmpleadoId = :empleadoId
          AND (:anio IS NULL OR n.anio = :anio)
          AND (:periodo IS NULL OR n.periodo = :periodo)
          AND (:fkConcepNominaId IS NULL OR n.fkConcepNominaId = :fkConcepNominaId)
        ORDER BY n.fechaNovedad DESC
        """)
    List<Novedad> findByEmpleadoYFiltros(
            @Param("empleadoId") Long empleadoId,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            @Param("fkConcepNominaId") Long fkConcepNominaId
    );

    // Para V19
    @Query(value = """
    SELECT
        nov.novedad_id,
        nov.anio,
        nov.periodo,
        emp.documento_emp,
        emp.nombres_emp,
        emp.apellidos_emp,
        nov.fecha_novedad,
        COALESCE(nov.fecha_inicio_ausen, nov.fecha_novedad) AS fecha_inicio,
        COALESCE(nov.fecha_fin_ausen,    nov.fecha_novedad) AS fecha_fin,
        nov.tipo_vacacion,
        nov.cantidad_dias_novedad,
        nov.observaciones,
        rnd.valor_result_concept
    FROM payroll.novedad nov
    JOIN master_data.empleado emp
        ON nov.fk_empleado_id = emp.empleado_id
    JOIN payroll.proceso_liquidacion pl
        ON nov.proceso_liquid = pl.proceso_liqui_id
    LEFT JOIN payroll.reporte_nomina_detalle rnd
        ON rnd.fk_novedad_id = nov.novedad_id
       AND rnd.fk_concep_nomina_id IN (:vacacionIds)
    WHERE emp.fk_id_empresa = :empresaId
      AND nov.fk_concep_nomina_id IN (:vacacionIds)
      AND emp.deleted_at IS NULL
      AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')
      AND (:anio IS NULL OR nov.anio = :anio)
      AND (:periodo IS NULL OR nov.periodo = :periodo)
      AND (:documento IS NULL OR emp.documento_emp
           LIKE CONCAT('%', CAST(:documento AS TEXT), '%'))
      AND (:nombres IS NULL OR LOWER(CONCAT(emp.nombres_emp, ' ', emp.apellidos_emp))
           LIKE LOWER(CONCAT('%', CAST(:nombres AS TEXT), '%')))
    ORDER BY emp.apellidos_emp ASC, nov.fecha_novedad DESC
    """,
            countQuery = """
    SELECT COUNT(nov.novedad_id)
    FROM payroll.novedad nov
    JOIN master_data.empleado emp
        ON nov.fk_empleado_id = emp.empleado_id
    JOIN payroll.proceso_liquidacion pl
        ON nov.proceso_liquid = pl.proceso_liqui_id
    WHERE emp.fk_id_empresa = :empresaId
      AND nov.fk_concep_nomina_id IN (:vacacionIds)
      AND emp.deleted_at IS NULL
      AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')
    """,
            nativeQuery = true)
    Page<Object[]> findVacacionesPorEmpresa(
            @Param("empresaId") Long empresaId,
            @Param("vacacionIds") List<Long> vacacionIds,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            @Param("documento") String documento,
            @Param("nombres") String nombres,
            Pageable pageable
    );
}
