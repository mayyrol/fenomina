package com.fenomina.historicos_service.service;

import com.fenomina.historicos_service.dto.nomina.*;
import com.fenomina.historicos_service.entity.master.Empleado;
import com.fenomina.historicos_service.entity.master.Empresa;
import com.fenomina.historicos_service.entity.payroll.NominaCabecera;
import com.fenomina.historicos_service.entity.payroll.ProcesoLiquidacion;
import com.fenomina.historicos_service.entity.payroll.ReporteNominaDetalle;
import com.fenomina.historicos_service.exception.AccesoNoAutorizadoException;
import com.fenomina.historicos_service.exception.ResourceNotFoundException;
import com.fenomina.historicos_service.repository.master.ConceptoNominaRepository;
import com.fenomina.historicos_service.repository.master.EmpleadoRepository;
import com.fenomina.historicos_service.repository.master.EmpresaRepository;
import com.fenomina.historicos_service.repository.payroll.NominaCabeceraRepository;
import com.fenomina.historicos_service.repository.payroll.ProcesoLiquidacionRepository;
import com.fenomina.historicos_service.repository.payroll.ReporteNominaDetalleRepository;
import com.fenomina.historicos_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteNominaService {

    private final NominaCabeceraRepository nominaCabeceraRepository;
    private final ReporteNominaDetalleRepository reporteNominaDetalleRepository;
    private final ProcesoLiquidacionRepository procesoLiquidacionRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EmpresaRepository empresaRepository;
    private final ConceptoNominaRepository conceptoNominaRepository;

    public ReporteNominaEmpleadoDTO getDesprendibleNomina(Long cabecNominaId) {

        NominaCabecera cabecera = nominaCabeceraRepository.findById(cabecNominaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NominaCabecera", cabecNominaId));

        validarAccesoEmpleado(cabecera.getFkEmpleadoId());

        Empleado empleado = empleadoRepository.findById(cabecera.getFkEmpleadoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empleado", cabecera.getFkEmpleadoId()));

        Empresa empresa = empresaRepository.findById(empleado.getFkIdEmpresa())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empresa", empleado.getFkIdEmpresa()));

        List<ReporteNominaDetalle> detalles =
                reporteNominaDetalleRepository.findByFkCabecNominaId(cabecNominaId);

        // Mapa de concepNominaId -> nombre del concepto para no hacer N queries
        List<com.fenomina.historicos_service.entity.master.ConceptoNomina> todosConceptos = conceptoNominaRepository.findAll();

        Map<Long, String> nombresConceptos = todosConceptos.stream()
                .collect(Collectors.toMap(
                        c -> c.getConcepNominaId(),
                        c -> c.getNombreConcepNomina()
                ));

        Map<Long, String> categoriasConceptos = todosConceptos.stream()
                .collect(Collectors.toMap(
                        c -> c.getConcepNominaId(),
                        c -> c.getCategoriaConcNomina()
                ));

        Map<Long, Boolean> esSalarioMap = todosConceptos.stream()
                .collect(Collectors.toMap(
                        c -> c.getConcepNominaId(),
                        c -> c.getEsSalario()
                ));

        Map<Long, Boolean> esInformativoMap = todosConceptos.stream()
                .collect(Collectors.toMap(
                        c -> c.getConcepNominaId(),
                        c -> c.getEsInformativo()
                ));

        // Filtrar conceptos: excluir los que son APORTE_PATRONAL
        // (el desprendible del empleado no muestra lo que paga el empleador)
        List<ConceptoNominaLineaDTO> lineas = detalles.stream()
                .filter(d -> {
                    String cat = categoriasConceptos.get(d.getFkConcepNominaId());
                    return cat != null && !cat.equals("APORTE_PATRONAL");
                })
                .map(d -> ConceptoNominaLineaDTO.builder()
                        .concepNominaId(d.getFkConcepNominaId())
                        .nombreConcepto(nombresConceptos.getOrDefault(
                                d.getFkConcepNominaId(), "Concepto " + d.getFkConcepNominaId()))
                        .categoria(categoriasConceptos.get(d.getFkConcepNominaId()))
                        .esSalario(esSalarioMap.getOrDefault(d.getFkConcepNominaId(), false))
                        .esInformativo(esInformativoMap.getOrDefault(d.getFkConcepNominaId(), false))
                        .cantidad(d.getCantidadConcept())
                        .baseCalculo(d.getBaseCalculoConcept())
                        .valorResultado(d.getValorResultConcept())
                        .build())
                .collect(Collectors.toList());

        return ReporteNominaEmpleadoDTO.builder()
                .nombreEmpresa(empresa.getNombreEmpresa())
                .nitEmpresa(empresa.getEmpresaNit())
                .anio(cabecera.getAnioCabecNomina())
                .periodo(cabecera.getPeriodoCotiNomina())
                .fechaCierreNomina(cabecera.getFechaCierreNomina())
                .nombresEmp(empleado.getNombresEmp())
                .apellidosEmp(empleado.getApellidosEmp())
                .documentoEmp(empleado.getDocumentoEmp())
                .tipoDocumento(empleado.getTipoDocumento())
                .salarioBascMensual(empleado.getSalarioBascMensual())
                .cargoEmp(empleado.getCargoEmp())
                .totalDevengado(cabecera.getTotalDevengadoEmp())
                .totalDeducciones(cabecera.getTotalDeduccionEmp())
                .netoAPagar(cabecera.getNetoNominaEmp())
                .conceptos(lineas)
                .build();
    }

    public Page<ReporteNominaEmpleadosDTO> getReporteNominaEmpleados(
            Long empresaId,
            Integer anio,
            Integer periodo,
            String documento,
            String nombres,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V2 - Reporte nómina empleados empresa={}, anio={}, periodo={}",
                empresaId, anio, periodo);

        return reporteNominaDetalleRepository
                .findReporteNominaEmpleados(empresaId, anio, periodo,
                        documento, nombres, pageable)
                .map(this::mapReporteNominaEmpleados);
    }

    // -------------------------------------------------------
    // V11: Total nóminas empresa agrupado por periodo
    // -------------------------------------------------------

    public Page<ReporteNominaTotalEmpresaDTO> getReporteNominaTotalEmpresa(
            Long empresaId,
            Integer anio,
            Integer periodo,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V11 - Total nóminas empresa={}, anio={}, periodo={}",
                empresaId, anio, periodo);

        return nominaCabeceraRepository
                .findTotalesNominaPorEmpresaYPeriodo(empresaId, anio, periodo, pageable)
                .map(this::mapReporteNominaTotalEmpresa);
    }

    public Page<EstadoNominaDTO> getEstadosNominas(
            Long empresaId,
            Integer anio,
            Integer periodo,
            String estado,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V21 - Estados nóminas empresa={}, estado={}", empresaId, estado);

        return nominaCabeceraRepository
                .findByEmpresaYFiltros(empresaId, anio, periodo, pageable)
                .map(nc -> mapEstadoNomina(nc, estado));
    }


    public Page<EstadoNominaDTO> getEstadosNominasBorradorYPagado(
            Long empresaId,
            Integer anio,
            Integer periodo,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V22 - Nóminas borrador/pagado empresa={}", empresaId);

        Page<NominaCabecera> borrador = nominaCabeceraRepository
                .findByEmpresaYFiltros(empresaId, anio, periodo, pageable);

        return borrador.map(nc -> mapEstadoNomina(nc, "BORRADOR"));
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

    private void validarAccesoEmpleado(Long empleadoId) {
        String role = SecurityUtils.getCurrentUserRole();
        if ("CLIENTE_EMPRESA".equals(role)) {
            Long empresaDelToken = SecurityUtils.getCurrentUserEmpresaId();
            Empleado emp = empleadoRepository.findById(empleadoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empleado", empleadoId));
            if (!emp.getFkIdEmpresa().equals(empresaDelToken)) {
                throw new AccesoNoAutorizadoException(
                        "No tiene acceso a los datos de este empleado");
            }
        }
    }

    private ReporteNominaEmpleadosDTO mapReporteNominaEmpleados(Object[] row) {
        // [0] documento_emp
        // [1] nombres_emp
        // [2] apellidos_emp
        // [3] salario_basc_mensual
        // [4] anio_cabec_nomina
        // [5] periodo_coti_nomina
        // [6] total_devengado_emp
        // [7] total_deduccion_emp
        // [8] neto_nomina_emp
        // [9] dias_laborados
        // [10] estado_proceso
        return ReporteNominaEmpleadosDTO.builder()
                .documentoEmp((String) row[0])
                .nombresEmp((String) row[1])
                .apellidosEmp((String) row[2])
                .salarioBascMensual(toBigDecimal(row[3]))
                .anio(toInteger(row[4]))
                .periodo(toInteger(row[5]))
                .totalDevengado(toBigDecimal(row[6]))
                .totalDeducciones(toBigDecimal(row[7]))
                .netoNomina(toBigDecimal(row[8]))
                .diasLaborados(toInteger(row[9]))
                .estadoProceso((String) row[10])
                .build();
    }

    private ReporteNominaTotalEmpresaDTO mapReporteNominaTotalEmpresa(Object[] row) {
        // Orden según SELECT en findTotalesNominaPorEmpresaYPeriodo:
        // [0] anio
        // [1] periodo
        // [2] fecha_inicio_periodo
        // [3] fecha_fin_periodo
        // [4] total_neto
        // [5] total_devengado
        // [6] total_deducciones
        // [7] total_costo_empresa
        // [8] total_empleados
        // La posición [9] es estado_proceso
        return ReporteNominaTotalEmpresaDTO.builder()
                .anio(toInteger(row[0]))
                .periodo(toInteger(row[1]))
                .fechaInicioPeriodo(toLocalDate(row[2]))
                .fechaFinPeriodo(toLocalDate(row[3]))
                .totalNeto(toBigDecimal(row[4]))
                .totalDevengado(toBigDecimal(row[5]))
                .totalDeducciones(toBigDecimal(row[6]))
                .totalCostoEmpresa(toBigDecimal(row[7]))
                .totalEmpleados(toLong(row[8]))
                .estadoProceso((String) row[9])
                .build();
    }

    private EstadoNominaDTO mapEstadoNomina(NominaCabecera nc, String estado) {
        Empleado emp = empleadoRepository.findById(nc.getFkEmpleadoId())
                .orElse(null);

        ProcesoLiquidacion proceso = procesoLiquidacionRepository
                .findById(nc.getFkProcesoLiquiId())
                .orElse(null);

        return EstadoNominaDTO.builder()
                .cabecNominaId(nc.getCabecNominaId())
                .empleadoId(nc.getFkEmpleadoId())
                .nombresEmp(emp != null ? emp.getNombresEmp() : null)
                .apellidosEmp(emp != null ? emp.getApellidosEmp() : null)
                .documentoEmp(emp != null ? emp.getDocumentoEmp() : null)
                .fechaIngresoEmp(emp != null ? emp.getFechaIngresoEmp() : null)
                .anio(nc.getAnioCabecNomina())
                .periodo(nc.getPeriodoCotiNomina())
                .netoNominaEmp(nc.getNetoNominaEmp())
                .estadoProceso(proceso != null ? proceso.getEstadoProcNomina() : estado)
                .fechaCierreNomina(nc.getFechaCierreNomina())
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
}
