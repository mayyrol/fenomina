package com.fenomina.historicos_service.repository.payroll;

import com.fenomina.historicos_service.entity.payroll.NominaCabecera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NominaCabeceraRepository extends JpaRepository<NominaCabecera, Long> {

    // Para reportes V21/V22: estados de nóminas por empresa
    @Query("""
        SELECT nc FROM NominaCabecera nc
        JOIN ProcesoLiquidacion pl ON nc.fkProcesoLiquiId = pl.procesoLiquiId
        WHERE pl.fkIdEmpresa = :empresaId
          AND (:anio IS NULL OR nc.anioCabecNomina = :anio)
          AND (:periodo IS NULL OR nc.periodoCotiNomina = :periodo)
          AND pl.estadoProcNomina IN ('PENDIENTE_PAGO', 'PAGADO')
        ORDER BY nc.anioCabecNomina DESC, nc.periodoCotiNomina DESC
        """)
    Page<NominaCabecera> findByEmpresaYFiltros(
            @Param("empresaId") Long empresaId,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            Pageable pageable
    );

    // Para V11: total nóminas por empresa agrupado por periodo
    @Query(value = """
        SELECT
            pl.anio,
            pl.periodo,
            pl.fecha_inicio_periodo,
            pl.fecha_fin_periodo,
            SUM(nc.neto_nomina_emp)        AS total_neto,
            SUM(nc.total_devengado_emp)    AS total_devengado,
            SUM(nc.total_deduccion_emp)    AS total_deducciones,
            SUM(nc.costo_total_empresa)    AS total_costo_empresa,
            COUNT(nc.cabec_nomina_id)      AS total_empleados,
            pl.estado_proc_nomina AS estado_proceso
        FROM payroll.nomina_cabecera nc
        JOIN payroll.proceso_liquidacion pl ON nc.fk_proceso_liqui_id = pl.proceso_liqui_id
        WHERE pl.fk_id_empresa = :empresaId
          AND nc.deleted_at IS NULL
          AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')
          AND (:anio IS NULL OR pl.anio = :anio)
          AND (:periodo IS NULL OR pl.periodo = :periodo)
        GROUP BY pl.anio, pl.periodo, pl.fecha_inicio_periodo, pl.fecha_fin_periodo, pl.estado_proc_nomina
        ORDER BY pl.anio DESC, pl.periodo DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT CONCAT(pl.anio, '-', pl.periodo))
        FROM payroll.nomina_cabecera nc
        JOIN payroll.proceso_liquidacion pl ON nc.fk_proceso_liqui_id = pl.proceso_liqui_id
        WHERE pl.fk_id_empresa = :empresaId AND nc.deleted_at IS NULL AND pl.estado_proc_nomina IN ('PENDIENTE_PAGO', 'PAGADO')
        """,
            nativeQuery = true)
    Page<Object[]> findTotalesNominaPorEmpresaYPeriodo(
            @Param("empresaId") Long empresaId,
            @Param("anio") Integer anio,
            @Param("periodo") Integer periodo,
            Pageable pageable
    );
}