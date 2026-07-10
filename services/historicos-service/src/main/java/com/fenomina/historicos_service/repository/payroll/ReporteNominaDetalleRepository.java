package com.fenomina.historicos_service.repository.payroll;

import com.fenomina.historicos_service.entity.payroll.ReporteNominaDetalle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteNominaDetalleRepository extends JpaRepository<ReporteNominaDetalle, Long> {

    // Para V1
    List<ReporteNominaDetalle> findByFkCabecNominaId(Long fkCabecNominaId);

    // Para V2
    @Query(value = """

            SELECT
                emp.documento_emp,
                emp.nombres_emp,
                emp.apellidos_emp,
                emp.salario_basc_mensual,
                nc.anio_cabec_nomina        AS anio,
                nc.periodo_coti_nomina      AS periodo,
                nc.total_devengado_emp,
                nc.total_deduccion_emp,
                nc.neto_nomina_emp,
                (SELECT rnd2.cantidad_concept
                 FROM payroll.reporte_nomina_detalle rnd2
                 WHERE rnd2.fk_cabec_nomina_id = nc.cabec_nomina_id
                   AND rnd2.fk_concep_nomina_id = 1
                 LIMIT 1) AS dias_laborados,
                pl.estado_proc_nomina AS estado_proceso
            FROM payroll.nomina_cabecera nc
            JOIN master_data.empleado emp ON nc.fk_empleado_id = emp.empleado_id
            JOIN payroll.proceso_liquidacion pl ON nc.fk_proceso_liqui_id = pl.proceso_liqui_id
            WHERE pl.fk_id_empresa = :empresaId
              AND nc.deleted_at IS NULL
              AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')
              AND emp.deleted_at IS NULL
              AND (:anio IS NULL OR nc.anio_cabec_nomina = :anio)
              AND (:periodo IS NULL OR nc.periodo_coti_nomina = :periodo)
              AND (:documento IS NULL OR emp.documento_emp LIKE CONCAT('%', CAST(:documento AS TEXT), '%'))
              AND (:nombres IS NULL OR LOWER(CONCAT(emp.nombres_emp, ' ', emp.apellidos_emp))
                   LIKE LOWER(CONCAT('%', CAST(:nombres AS TEXT), '%')))
            ORDER BY emp.apellidos_emp ASC, nc.anio_cabec_nomina DESC
        """,
            countQuery = """
        SELECT COUNT(nc.cabec_nomina_id)
        FROM payroll.nomina_cabecera nc
        JOIN master_data.empleado emp ON nc.fk_empleado_id = emp.empleado_id
        JOIN payroll.proceso_liquidacion pl ON nc.fk_proceso_liqui_id = pl.proceso_liqui_id
        WHERE pl.fk_id_empresa = :empresaId
          AND nc.deleted_at IS NULL
          AND emp.deleted_at IS NULL
          AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')
        """,
            nativeQuery = true)
    Page<Object[]> findReporteNominaEmpleados(
            @Param("empresaId") Long empresaId,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            @Param("documento") String documento,
            @Param("nombres") String nombres,
            Pageable pageable
    );

    @Query(value = """
    SELECT
        pl.anio,
        pl.periodo,
        rnd.fk_concep_nomina_id,
        SUM(rnd.valor_result_concept) AS total_valor
    FROM payroll.reporte_nomina_detalle rnd
    JOIN payroll.nomina_cabecera nc
        ON rnd.fk_cabec_nomina_id = nc.cabec_nomina_id
    JOIN payroll.proceso_liquidacion pl
        ON nc.fk_proceso_liqui_id = pl.proceso_liqui_id
    JOIN master_data.empleado emp
        ON nc.fk_empleado_id = emp.empleado_id
    WHERE pl.fk_id_empresa = :empresaId
      AND rnd.fk_concep_nomina_id IN (:conceptoIds)
      AND nc.deleted_at IS NULL
      AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')
      AND emp.deleted_at IS NULL
      AND (:anio IS NULL OR pl.anio = :anio)
      AND (:periodo IS NULL OR pl.periodo = :periodo)
    GROUP BY pl.anio, pl.periodo, rnd.fk_concep_nomina_id
    ORDER BY pl.anio DESC, pl.periodo DESC
    """,
            nativeQuery = true)
    List<Object[]> findTotalesPorConceptoIdsYPeriodo(
            @Param("empresaId") Long empresaId,
            @Param("conceptoIds") List<Long> conceptoIds,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo
    );

    @Query(value = """
    SELECT
        pl.anio,
        pl.periodo,
        emp.documento_emp,
        emp.nombres_emp,
        emp.apellidos_emp,
        emp.fecha_ingreso_emp,
        rnd.fk_concep_nomina_id,
        rnd.valor_result_concept
    FROM payroll.reporte_nomina_detalle rnd
    JOIN payroll.nomina_cabecera nc
        ON rnd.fk_cabec_nomina_id = nc.cabec_nomina_id
    JOIN payroll.proceso_liquidacion pl
        ON nc.fk_proceso_liqui_id = pl.proceso_liqui_id
    JOIN master_data.empleado emp
        ON nc.fk_empleado_id = emp.empleado_id
    WHERE pl.fk_id_empresa = :empresaId
      AND rnd.fk_concep_nomina_id IN (:conceptoIds)
      AND nc.deleted_at IS NULL
      AND emp.deleted_at IS NULL
      AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')   
      AND (:anio IS NULL OR pl.anio = :anio)
      AND (:periodo IS NULL OR pl.periodo = :periodo)
      AND (:documento IS NULL OR emp.documento_emp
           LIKE CONCAT('%', CAST(:documento AS TEXT), '%'))
      AND (:nombres IS NULL OR LOWER(CONCAT(emp.nombres_emp, ' ', emp.apellidos_emp))
           LIKE LOWER(CONCAT('%', CAST(:nombres AS TEXT), '%')))
    ORDER BY emp.apellidos_emp ASC, pl.anio DESC, pl.periodo DESC
    """,
            countQuery = """
                    SELECT COUNT(DISTINCT CONCAT(emp.documento_emp, '-', pl.anio, '-', pl.periodo))
                    FROM payroll.reporte_nomina_detalle rnd
                    JOIN payroll.nomina_cabecera nc
                        ON rnd.fk_cabec_nomina_id = nc.cabec_nomina_id
                    JOIN payroll.proceso_liquidacion pl
                        ON nc.fk_proceso_liqui_id = pl.proceso_liqui_id
                    JOIN master_data.empleado emp
                        ON nc.fk_empleado_id = emp.empleado_id
                    WHERE pl.fk_id_empresa = :empresaId
                      AND rnd.fk_concep_nomina_id IN (:conceptoIds)
                      AND nc.deleted_at IS NULL
                      AND emp.deleted_at IS NULL
                      AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')
    """,
            nativeQuery = true)
    Page<Object[]> findDetallesPorConceptoIdsYEmpleado(
            @Param("empresaId") Long empresaId,
            @Param("conceptoIds") List<Long> conceptoIds,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            @Param("documento") String documento,
            @Param("nombres") String nombres,
            Pageable pageable
    );

    // Para V12 y V14 y V16
    @Query(value = """
    SELECT
        pl.anio,
        pl.periodo,
        pl.fecha_inicio_periodo,
        emp.documento_emp,
        emp.nombres_emp,
        emp.apellidos_emp,
        rnd.fk_concep_nomina_id,
        rnd.cantidad_concept,
        rnd.valor_result_concept,
        nov.observaciones,
        nov.tipo_vacacion
    FROM payroll.reporte_nomina_detalle rnd
    JOIN payroll.nomina_cabecera nc
        ON rnd.fk_cabec_nomina_id = nc.cabec_nomina_id
    JOIN payroll.proceso_liquidacion pl
        ON nc.fk_proceso_liqui_id = pl.proceso_liqui_id
    JOIN master_data.empleado emp
        ON nc.fk_empleado_id = emp.empleado_id
    LEFT JOIN payroll.novedad nov
        ON rnd.fk_novedad_id = nov.novedad_id
    WHERE pl.fk_id_empresa = :empresaId
      AND rnd.fk_concep_nomina_id IN (:conceptoIds)
      AND nc.deleted_at IS NULL
      AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')    
      AND emp.deleted_at IS NULL     
      AND (:anio IS NULL OR pl.anio = :anio)
      AND (:periodo IS NULL OR pl.periodo = :periodo)
      AND (:documento IS NULL OR emp.documento_emp
           LIKE CONCAT('%', CAST(:documento AS TEXT), '%'))
      AND (:nombres IS NULL OR LOWER(CONCAT(emp.nombres_emp, ' ', emp.apellidos_emp))
           LIKE LOWER(CONCAT('%', CAST(:nombres AS TEXT), '%')))
    ORDER BY emp.apellidos_emp ASC, pl.anio DESC, pl.periodo DESC
    """,
            countQuery = """
                    SELECT COUNT(DISTINCT CONCAT(emp.documento_emp, '-', pl.anio, '-', pl.periodo))
                    FROM payroll.reporte_nomina_detalle rnd
                    JOIN payroll.nomina_cabecera nc
                        ON rnd.fk_cabec_nomina_id = nc.cabec_nomina_id
                    JOIN payroll.proceso_liquidacion pl
                        ON nc.fk_proceso_liqui_id = pl.proceso_liqui_id
                    JOIN master_data.empleado emp
                        ON nc.fk_empleado_id = emp.empleado_id
                    WHERE pl.fk_id_empresa = :empresaId
                      AND rnd.fk_concep_nomina_id IN (:conceptoIds)
                      AND nc.deleted_at IS NULL
                      AND emp.deleted_at IS NULL
                      AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')
    """,
            nativeQuery = true)
    Page<Object[]> findDetalleConceptosVariosPorEmpleado(
            @Param("empresaId") Long empresaId,
            @Param("conceptoIds") List<Long> conceptoIds,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            @Param("documento") String documento,
            @Param("nombres") String nombres,
            Pageable pageable
    );

    // Para V18
    @Query(value = """
    SELECT
        pl.anio,
        pl.periodo,
        emp.documento_emp,
        emp.nombres_emp,
        emp.apellidos_emp,
        SUM(rnd.valor_result_concept) AS total_retefuente
    FROM payroll.reporte_nomina_detalle rnd
    JOIN payroll.nomina_cabecera nc
        ON rnd.fk_cabec_nomina_id = nc.cabec_nomina_id
    JOIN payroll.proceso_liquidacion pl
        ON nc.fk_proceso_liqui_id = pl.proceso_liqui_id
    JOIN master_data.empleado emp
        ON nc.fk_empleado_id = emp.empleado_id
    WHERE pl.fk_id_empresa = :empresaId
      AND rnd.fk_concep_nomina_id = :conceptoId
      AND nc.deleted_at IS NULL
      AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')  
      AND emp.deleted_at IS NULL
      AND (:anio IS NULL OR pl.anio = :anio)
      AND (:periodo IS NULL OR pl.periodo = :periodo)
      AND (:documento IS NULL OR emp.documento_emp
           LIKE CONCAT('%', CAST(:documento AS TEXT), '%'))
      AND (:nombres IS NULL OR LOWER(CONCAT(emp.nombres_emp, ' ', emp.apellidos_emp))
           LIKE LOWER(CONCAT('%', CAST(:nombres AS TEXT), '%')))
    GROUP BY pl.anio, pl.periodo, emp.documento_emp,
             emp.nombres_emp, emp.apellidos_emp
    ORDER BY emp.apellidos_emp ASC
    """,
            countQuery = """
    SELECT COUNT(DISTINCT emp.empleado_id)
    FROM payroll.reporte_nomina_detalle rnd
    JOIN payroll.nomina_cabecera nc
        ON rnd.fk_cabec_nomina_id = nc.cabec_nomina_id
    JOIN payroll.proceso_liquidacion pl
        ON nc.fk_proceso_liqui_id = pl.proceso_liqui_id
    JOIN master_data.empleado emp
        ON nc.fk_empleado_id = emp.empleado_id
    WHERE pl.fk_id_empresa = :empresaId
      AND rnd.fk_concep_nomina_id = :conceptoId
      AND nc.deleted_at IS NULL AND emp.deleted_at IS NULL
       AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')
    """,
            nativeQuery = true)
    Page<Object[]> findRetefuentePorEmpleado(
            @Param("empresaId") Long empresaId,
            @Param("conceptoId") Long conceptoId,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            @Param("documento") String documento,
            @Param("nombres") String nombres,
            Pageable pageable
    );

    // Sin paginación para seguridad social y parafiscales x empleado
    @Query(value = """
    SELECT
        pl.anio,
        pl.periodo,
        emp.documento_emp,
        emp.nombres_emp,
        emp.apellidos_emp,
        emp.fecha_ingreso_emp,
        rnd.fk_concep_nomina_id,
        rnd.valor_result_concept
    FROM payroll.reporte_nomina_detalle rnd
    JOIN payroll.nomina_cabecera nc
        ON rnd.fk_cabec_nomina_id = nc.cabec_nomina_id
    JOIN payroll.proceso_liquidacion pl
        ON nc.fk_proceso_liqui_id = pl.proceso_liqui_id
    JOIN master_data.empleado emp
        ON nc.fk_empleado_id = emp.empleado_id
    WHERE pl.fk_id_empresa = :empresaId
      AND rnd.fk_concep_nomina_id IN (:conceptoIds)
      AND nc.deleted_at IS NULL
      AND emp.deleted_at IS NULL
      AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')
      AND (:anio IS NULL OR pl.anio = :anio)
      AND (:periodo IS NULL OR pl.periodo = :periodo)
      AND (:documento IS NULL OR emp.documento_emp
           LIKE CONCAT('%', CAST(:documento AS TEXT), '%'))
      AND (:nombres IS NULL OR LOWER(CONCAT(emp.nombres_emp, ' ', emp.apellidos_emp))
           LIKE LOWER(CONCAT('%', CAST(:nombres AS TEXT), '%')))
    ORDER BY emp.apellidos_emp ASC, pl.anio DESC, pl.periodo DESC
    """,
            nativeQuery = true)
    List<Object[]> findDetallesPorConceptoIdsYEmpleadoSinPaginar(
            @Param("empresaId") Long empresaId,
            @Param("conceptoIds") List<Long> conceptoIds,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            @Param("documento") String documento,
            @Param("nombres") String nombres
    );

}
