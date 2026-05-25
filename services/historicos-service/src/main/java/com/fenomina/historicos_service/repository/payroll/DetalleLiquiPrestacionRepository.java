package com.fenomina.historicos_service.repository.payroll;

import com.fenomina.historicos_service.entity.payroll.DetalleLiquiPrestacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleLiquiPrestacionRepository extends JpaRepository<DetalleLiquiPrestacion, Long> {

    List<DetalleLiquiPrestacion> findByFkCabeLiquiPrestacionId(Long fkCabeLiquiPrestacionId);

    // Para V3 y V5: tabla general primas/cesantías por empleado y empresa
    @Query(value = """
        SELECT
            emp.documento_emp,
            emp.nombres_emp,
            emp.apellidos_emp,
            emp.salario_basc_mensual,
            emp.tiene_aux_transporte,
            emp.fondo_pension_emp,
            emp.fecha_ingreso_emp,
            clp.anio_liqui_prestacion,
            clp.periodo_liqui_prestacion,
            clp.finicio_general_liqui_prest,
            clp.ffinal_general_liqui_prest,
            dlp.fecha_inicio_corte_emp,
            dlp.fecha_fin_corte_emp,
            dlp.dias_liquidados_int,
            dlp.salario_fijo_momento,
            dlp.base_liqui_total,
            dlp.valor_neto_presta,
            dlp.valor_int_cesantias,
            dlp.promedio_var_periodo,
            cn.nombre_concep_nomina,
            cn.concep_nomina_id
        FROM payroll.detalle_liqui_prestacion dlp
        JOIN payroll.cabecera_liqui_prestacion clp
            ON dlp.fk_cabe_liqui_prestacion_id = clp.cabe_liqui_prestacion_id
        JOIN payroll.proceso_liquidacion pl
            ON clp.fk_proceso_liqui_id = pl.proceso_liqui_id
        JOIN master_data.empleado emp
            ON dlp.fk_empleado_id = emp.empleado_id
        JOIN master_data.concepto_nomina cn
            ON dlp.fk_concep_nomina_id = cn.concep_nomina_id
        WHERE pl.fk_id_empresa = :empresaId
          AND clp.deleted_at IS NULL
          AND emp.deleted_at IS NULL
          AND pl.estado_proc_nomina = 'PAGADO'
          AND (:tipoProceso IS NULL OR pl.tipo_proceso = :tipoProceso)
          AND (:anio IS NULL OR clp.anio_liqui_prestacion = :anio)
          AND (:periodo IS NULL OR clp.periodo_liqui_prestacion = :periodo)
          AND (:documento IS NULL OR emp.documento_emp LIKE CONCAT('%', CAST(:documento AS TEXT), '%'))
          AND (:nombres IS NULL OR LOWER(CONCAT(emp.nombres_emp, ' ', emp.apellidos_emp))
               LIKE LOWER(CONCAT('%', CAST(:nombres AS TEXT), '%')))
        ORDER BY emp.apellidos_emp ASC
        """,
            countQuery = """
        SELECT COUNT(dlp.detalle_prestacion_id)
        FROM payroll.detalle_liqui_prestacion dlp
        JOIN payroll.cabecera_liqui_prestacion clp
            ON dlp.fk_cabe_liqui_prestacion_id = clp.cabe_liqui_prestacion_id
        JOIN payroll.proceso_liquidacion pl
            ON clp.fk_proceso_liqui_id = pl.proceso_liqui_id
        JOIN master_data.empleado emp ON dlp.fk_empleado_id = emp.empleado_id
        WHERE pl.fk_id_empresa = :empresaId
          AND clp.deleted_at IS NULL AND emp.deleted_at IS NULL
        """,
            nativeQuery = true)
    Page<Object[]> findReportePrestacionesPorEmpresa(
            @Param("empresaId") Long empresaId,
            @Param("tipoProceso") String tipoProceso,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            @Param("documento") String documento,
            @Param("nombres") String nombres,
            Pageable pageable
    );

    // Para V3.1 y V5.1: totales por empresa y periodo
    @Query(value = """
        SELECT
            clp.anio_liqui_prestacion,
            clp.periodo_liqui_prestacion,
            pl.tipo_proceso,
            SUM(dlp.valor_neto_presta)      AS total_neto,
            SUM(dlp.valor_int_cesantias)    AS total_intereses_cesantias,
            COUNT(DISTINCT dlp.fk_empleado_id) AS total_empleados
        FROM payroll.detalle_liqui_prestacion dlp
        JOIN payroll.cabecera_liqui_prestacion clp
            ON dlp.fk_cabe_liqui_prestacion_id = clp.cabe_liqui_prestacion_id
        JOIN payroll.proceso_liquidacion pl
            ON clp.fk_proceso_liqui_id = pl.proceso_liqui_id
        WHERE pl.fk_id_empresa = :empresaId
          AND clp.deleted_at IS NULL
          AND pl.estado_proc_nomina = 'PAGADO'        
          AND (:tipoProceso IS NULL OR pl.tipo_proceso = :tipoProceso)
          AND (:anio IS NULL OR clp.anio_liqui_prestacion = :anio)
          AND (:periodo IS NULL OR clp.periodo_liqui_prestacion = :periodo)
        GROUP BY clp.anio_liqui_prestacion, clp.periodo_liqui_prestacion, pl.tipo_proceso
        ORDER BY clp.anio_liqui_prestacion DESC, clp.periodo_liqui_prestacion DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT CONCAT(clp.anio_liqui_prestacion, '-', clp.periodo_liqui_prestacion))
        FROM payroll.detalle_liqui_prestacion dlp
        JOIN payroll.cabecera_liqui_prestacion clp
            ON dlp.fk_cabe_liqui_prestacion_id = clp.cabe_liqui_prestacion_id
        JOIN payroll.proceso_liquidacion pl ON clp.fk_proceso_liqui_id = pl.proceso_liqui_id
        WHERE pl.fk_id_empresa = :empresaId AND clp.deleted_at IS NULL
        """,
            nativeQuery = true)
    Page<Object[]> findTotalesPrestacionesPorEmpresaYPeriodo(
            @Param("empresaId") Long empresaId,
            @Param("tipoProceso") String tipoProceso,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            Pageable pageable
    );
}
