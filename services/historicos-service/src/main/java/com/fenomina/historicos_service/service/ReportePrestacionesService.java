package com.fenomina.historicos_service.service;

import com.fenomina.historicos_service.dto.prestaciones.*;
import com.fenomina.historicos_service.exception.AccesoNoAutorizadoException;
import com.fenomina.historicos_service.repository.payroll.DetalleLiquiPrestacionRepository;
import com.fenomina.historicos_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportePrestacionesService {

    private final DetalleLiquiPrestacionRepository detalleLiquiPrestacionRepository;

    public Page<ReportePrimaEmpresaDTO> getReportePrimasEmpresa(
            Long empresaId,
            Integer anio,
            Integer periodo,
            String documento,
            String nombres,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V3 - Reporte primas empresa={}, anio={}, periodo={}",
                empresaId, anio, periodo);

        return detalleLiquiPrestacionRepository
                .findReportePrestacionesPorEmpresa(
                        empresaId, "PRIMA_SEMESTRAL",
                        anio, periodo, documento, nombres, pageable)
                .map(this::mapReportePrimaEmpresa);
    }

    public Page<ReporteTotalesPrimasDTO> getTotalesPrimasEmpresa(
            Long empresaId,
            Integer anio,
            Integer periodo,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V3.1 - Totales primas empresa={}, anio={}, periodo={}",
                empresaId, anio, periodo);

        return detalleLiquiPrestacionRepository
                .findTotalesPrestacionesPorEmpresaYPeriodo(
                        empresaId, "PRIMA_SEMESTRAL",
                        anio, periodo, pageable)
                .map(this::mapTotalesPrimas);
    }

    public Page<ReporteCesantiasEmpresaDTO> getReporteCesantiasEmpresa(
            Long empresaId,
            Integer anio,
            Integer periodo,
            String documento,
            String nombres,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V5 - Reporte cesantías empresa={}, anio={}, periodo={}",
                empresaId, anio, periodo);

        return detalleLiquiPrestacionRepository
                .findReportePrestacionesPorEmpresa(
                        empresaId, "CESANTIAS_ANUAL",
                        anio, periodo, documento, nombres, pageable)
                .map(this::mapReporteCesantiasEmpresa);
    }

    public Page<ReporteTotalesCesantiasDTO> getTotalesCesantiasEmpresa(
            Long empresaId,
            Integer anio,
            Integer periodo,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V5.1 - Totales cesantías empresa={}, anio={}, periodo={}",
                empresaId, anio, periodo);

        return detalleLiquiPrestacionRepository
                .findTotalesPrestacionesPorEmpresaYPeriodo(
                        empresaId, "CESANTIAS_ANUAL",
                        anio, periodo, pageable)
                .map(this::mapTotalesCesantias);
    }

    private void validarAccesoEmpresa(Long empresaId) {
        String role = SecurityUtils.getCurrentUserRole();
        if ("CLIENTE_EMPRESA".equals(role)) {
            Long empresaDelToken = SecurityUtils.getCurrentUserEmpresaId();
            if (!empresaId.equals(empresaDelToken)) {
                throw new AccesoNoAutorizadoException(
                        "No tiene acceso a los reportes de esta empresa");
            }
        }
    }

    private ReportePrimaEmpresaDTO mapReportePrimaEmpresa(Object[] row) {
        // Orden según SELECT en findReportePrestacionesPorEmpresa:
        // [0]  documento_emp
        // [1]  nombres_emp
        // [2]  apellidos_emp
        // [3]  salario_basc_mensual
        // [4]  tiene_aux_transporte
        // [5]  fondo_pension_emp
        // [6]  fecha_ingreso_emp
        // [7]  anio_liqui_prestacion
        // [8]  periodo_liqui_prestacion
        // [9]  finicio_general_liqui_prest
        // [10] ffinal_general_liqui_prest
        // [11] fecha_inicio_corte_emp
        // [12] fecha_fin_corte_emp
        // [13] dias_liquidados_int
        // [14] salario_fijo_momento
        // [15] base_liqui_total
        // [16] valor_neto_presta
        // [17] valor_int_cesantias
        // [18] promedio_var_periodo
        // [19] promedio_aux_transporte
        // [20] nombre_concep_nomina
        // [21] concep_nomina_id
        // [22] estado_proc_nomina
        return ReportePrimaEmpresaDTO.builder()
                .documentoEmp((String) row[0])
                .nombresEmp((String) row[1])
                .apellidosEmp((String) row[2])
                .salarioBase(toBigDecimal(row[14]))
                .tieneAuxTransporte(toBoolean(row[4]))
                .promedioAuxTransporte(toBigDecimal(row[19]))
                .anioLiqui(toInteger(row[7]))
                .periodoLiqui(toInteger(row[8]))
                .finicioGeneral(toLocalDate(row[9]))
                .ffinalGeneral(toLocalDate(row[10]))
                .fechaInicioCorte(toLocalDate(row[11]))
                .fechaFinCorte(toLocalDate(row[12]))
                .diasLiquidados(toInteger(row[13]))
                .baseLiquiTotal(toBigDecimal(row[15]))
                .valorNetoPrima(toBigDecimal(row[16]))
                .promedioVarPeriodo(toBigDecimal(row[18]))
                .estadoProceso((String) row[22])
                .build();
    }

    private ReporteTotalesPrimasDTO mapTotalesPrimas(Object[] row) {
        // Orden según SELECT en findTotalesPrestacionesPorEmpresaYPeriodo:
        // [0] anio_liqui_prestacion
        // [1] periodo_liqui_prestacion
        // [2] tipo_proceso
        // [3] total_neto
        // [4] total_intereses_cesantias
        // [5] total_empleados
        return ReporteTotalesPrimasDTO.builder()
                .anio(toInteger(row[0]))
                .periodo(toInteger(row[1]))
                .totalNetoPrimas(toBigDecimal(row[3]))
                .totalEmpleados(toLong(row[5]))
                .estadoProceso((String) row[6])
                .build();
    }

    private ReporteCesantiasEmpresaDTO mapReporteCesantiasEmpresa(Object[] row) {
        return ReporteCesantiasEmpresaDTO.builder()
                .documentoEmp((String) row[0])
                .nombresEmp((String) row[1])
                .apellidosEmp((String) row[2])
                .salarioBase(toBigDecimal(row[3]))           // salario_basc_mensual del empleado
                .tieneAuxTransporte(toBoolean(row[4]))
                .fondoCesantiasEmp((String) row[5])
                .fechaIngresoEmp(toLocalDate(row[6]))
                .anioLiqui(toInteger(row[7]))
                .periodoLiqui(toInteger(row[8]))
                .finicioGeneral(toLocalDate(row[9]))
                .ffinalGeneral(toLocalDate(row[10]))
                .fechaInicioCorte(toLocalDate(row[11]))
                .fechaFinCorte(toLocalDate(row[12]))
                .diasLiquidados(toInteger(row[13]))
                .salarioFijoMomento(toBigDecimal(row[14]))
                .baseLiquiTotal(toBigDecimal(row[15]))
                .cesantias(toBigDecimal(row[16]))
                .interesesCesantias(toBigDecimal(row[17]))
                .promedioAuxTransporte(toBigDecimal(row[19]))
                .estadoProceso((String) row[22])
                .build();
    }

    private ReporteTotalesCesantiasDTO mapTotalesCesantias(Object[] row) {
        // [0] anio_liqui_prestacion
        // [1] periodo_liqui_prestacion
        // [2] tipo_proceso
        // [3] total_neto          -> total cesantías
        // [4] total_intereses_cesantias
        // [5] total_empleados
        return ReporteTotalesCesantiasDTO.builder()
                .anio(toInteger(row[0]))
                .periodo(toInteger(row[1]))
                .totalCesantias(toBigDecimal(row[3]))
                .totalInteresesCesantias(toBigDecimal(row[4]))
                .totalEmpleados(toLong(row[5]))
                .estadoProceso((String) row[6])
                .build();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        return ((Number) value).intValue();
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        return ((Number) value).longValue();
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        return LocalDate.parse(value.toString());
    }

    private Boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }
}
