package com.fenomina.historicos_service.service;

import com.fenomina.historicos_service.config.ConceptoNominaIds;
import com.fenomina.historicos_service.dto.conceptos.*;
import com.fenomina.historicos_service.exception.AccesoNoAutorizadoException;
import com.fenomina.historicos_service.repository.payroll.NovedadRepository;
import com.fenomina.historicos_service.repository.payroll.ReporteNominaDetalleRepository;
import com.fenomina.historicos_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteConceptosService {

    private final ReporteNominaDetalleRepository reporteNominaDetalleRepository;
    private final NovedadRepository novedadRepository;

    private static final List<Long> IDS_HORAS_RECARGOS = List.of(
            ConceptoNominaIds.RECARGO_NOCTURNO_LUN_SAB,
            ConceptoNominaIds.RECARGO_DIURNO_DOM_FEST,
            ConceptoNominaIds.RECARGO_NOCTURNO_DOM_FEST,
            ConceptoNominaIds.HORA_EXTRA_DIURNA_LUN_SAB,
            ConceptoNominaIds.HORA_EXTRA_NOCTURNA_LUN_SAB,
            ConceptoNominaIds.HORA_EXTRA_DIURNA_DOM_FEST,
            ConceptoNominaIds.HORA_EXTRA_NOCTURNA_DOM_FEST
    );

    private static final List<Long> IDS_INCAPACIDADES = List.of(
            ConceptoNominaIds.INCAPACIDAD_COMUN,
            ConceptoNominaIds.INCAPACIDAD_LABORAL
    );

    private static final List<Long> IDS_LICENCIAS = List.of(
            ConceptoNominaIds.LICENCIA_MATERNIDAD,
            ConceptoNominaIds.LICENCIA_PATERNIDAD,
            ConceptoNominaIds.LICENCIA_CALAMIDAD,
            ConceptoNominaIds.LICENCIA_MATRIMONIO,
            ConceptoNominaIds.LICENCIA_ISAAC,
            ConceptoNominaIds.LICENCIA_SUFRAGIO,
            ConceptoNominaIds.CARGOS_TRANSITORIOS,
            ConceptoNominaIds.CITACIONES_JUDICIALES,
            ConceptoNominaIds.OTROS_PERMISOS_REMUNERADOS,
            ConceptoNominaIds.LICENCIAS_NO_REMUNERADAS
    );

    private static final List<Long> IDS_VACACIONES = List.of(
            ConceptoNominaIds.VACACIONES_DISFRUTADAS,
            ConceptoNominaIds.VACACIONES_COMPENSADAS
    );

    public Page<ReporteHorasRecargosDTO> getHorasRecargosPorEmpleado(
            Long empresaId,
            Integer anio,
            Integer periodo,
            String documento,
            String nombres,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V12 - Horas recargos empresa={}, anio={}, periodo={}",
                empresaId, anio, periodo);

        Page<Object[]> rawPage = reporteNominaDetalleRepository
                .findDetalleConceptosVariosPorEmpleado(
                        empresaId, IDS_HORAS_RECARGOS,
                        anio, periodo, documento, nombres, pageable);

        Map<String, List<Object[]>> porEmpleado = rawPage.getContent().stream()
                .collect(Collectors.groupingBy(
                        row -> row[3] + "-" + toInteger(row[0]) + "-" + toInteger(row[1])
                ));

        List<ReporteHorasRecargosDTO> dtos = porEmpleado.values().stream()
                .map(filas -> {
                    Object[] primera = filas.get(0);
                    Map<Long, BigDecimal> vals  = sumarValoresPorConcepto(filas, 6, 8);
                    Map<Long, BigDecimal> horas = sumarValoresPorConcepto(filas, 6, 7);

                    BigDecimal totalHorasExtra = vals.values().stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return ReporteHorasRecargosDTO.builder()
                            .anio(toInteger(primera[0]))
                            .periodo(toInteger(primera[1]))
                            .fechaPeriodo(toLocalDate(primera[2]))
                            .documentoEmp((String) primera[3])
                            .nombresEmp((String) primera[4])
                            .apellidosEmp((String) primera[5])
                            .horasRecargoNocturnoLunSab(horas.getOrDefault(
                                    ConceptoNominaIds.RECARGO_NOCTURNO_LUN_SAB, BigDecimal.ZERO))
                            .valorRecargoNocturnoLunSab(vals.getOrDefault(
                                    ConceptoNominaIds.RECARGO_NOCTURNO_LUN_SAB, BigDecimal.ZERO))
                            .horasRecargoDiurnoDomFest(horas.getOrDefault(
                                    ConceptoNominaIds.RECARGO_DIURNO_DOM_FEST, BigDecimal.ZERO))
                            .valorRecargoDiurnoDomFest(vals.getOrDefault(
                                    ConceptoNominaIds.RECARGO_DIURNO_DOM_FEST, BigDecimal.ZERO))
                            .horasRecargoNocturnoDomFest(horas.getOrDefault(
                                    ConceptoNominaIds.RECARGO_NOCTURNO_DOM_FEST, BigDecimal.ZERO))
                            .valorRecargoNocturnoDomFest(vals.getOrDefault(
                                    ConceptoNominaIds.RECARGO_NOCTURNO_DOM_FEST, BigDecimal.ZERO))
                            .horasExtraDiurnaLunSab(horas.getOrDefault(
                                    ConceptoNominaIds.HORA_EXTRA_DIURNA_LUN_SAB, BigDecimal.ZERO))
                            .valorExtraDiurnaLunSab(vals.getOrDefault(
                                    ConceptoNominaIds.HORA_EXTRA_DIURNA_LUN_SAB, BigDecimal.ZERO))
                            .horasExtraNocturnaLunSab(horas.getOrDefault(
                                    ConceptoNominaIds.HORA_EXTRA_NOCTURNA_LUN_SAB, BigDecimal.ZERO))
                            .valorExtraNocturnaLunSab(vals.getOrDefault(
                                    ConceptoNominaIds.HORA_EXTRA_NOCTURNA_LUN_SAB, BigDecimal.ZERO))
                            .horasExtraDiurnaDomFest(horas.getOrDefault(
                                    ConceptoNominaIds.HORA_EXTRA_DIURNA_DOM_FEST, BigDecimal.ZERO))
                            .valorExtraDiurnaDomFest(vals.getOrDefault(
                                    ConceptoNominaIds.HORA_EXTRA_DIURNA_DOM_FEST, BigDecimal.ZERO))
                            .horasExtraNocturnaDomFest(horas.getOrDefault(
                                    ConceptoNominaIds.HORA_EXTRA_NOCTURNA_DOM_FEST, BigDecimal.ZERO))
                            .valorExtraNocturnaDomFest(vals.getOrDefault(
                                    ConceptoNominaIds.HORA_EXTRA_NOCTURNA_DOM_FEST, BigDecimal.ZERO))
                            .totalHorasExtraYRecargos(totalHorasExtra)
                            .build();
                })
                .sorted(Comparator.comparing(ReporteHorasRecargosDTO::getApellidosEmp))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, rawPage.getTotalElements());
    }

    public Page<ReporteHorasRecargosTotalDTO> getHorasRecargosTotalEmpresa(
            Long empresaId,
            Integer anio,
            Integer periodo,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V13 - Horas recargos total empresa={}", empresaId);

        List<Object[]> rawData = reporteNominaDetalleRepository
                .findTotalesPorConceptoIdsYPeriodo(
                        empresaId, IDS_HORAS_RECARGOS, anio, periodo);

        // Agrupar por periodo y sumar todos los conceptos
        Map<String, Object[]> metadataPorPeriodo = new LinkedHashMap<>();
        Map<String, BigDecimal> totalPorPeriodo  = new LinkedHashMap<>();

        for (Object[] row : rawData) {
            String key = toInteger(row[0]) + "-" + toInteger(row[1]);
            metadataPorPeriodo.putIfAbsent(key, row);
            totalPorPeriodo.merge(key, toBigDecimal(row[3]), BigDecimal::add);
        }

        List<ReporteHorasRecargosTotalDTO> resultado = totalPorPeriodo.entrySet().stream()
                .map(entry -> {
                    Object[] meta = metadataPorPeriodo.get(entry.getKey());
                    return ReporteHorasRecargosTotalDTO.builder()
                            .anio(toInteger(meta[0]))
                            .periodo(toInteger(meta[1]))
                            .fechaPeriodo(null) // se resuelve desde proceso_liquidacion
                            .totalHorasExtraYRecargosEmpresa(entry.getValue())
                            .build();
                })
                .sorted(Comparator.comparingInt(ReporteHorasRecargosTotalDTO::getAnio).reversed()
                        .thenComparingInt(ReporteHorasRecargosTotalDTO::getPeriodo).reversed())
                .collect(Collectors.toList());

        return paginar(resultado, pageable);
    }

    public Page<ReporteIncapacidadesDTO> getIncapacidadesPorEmpleado(
            Long empresaId,
            Integer anio,
            Integer periodo,
            String documento,
            String nombres,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V14 - Incapacidades empresa={}", empresaId);

        Page<Object[]> rawPage = reporteNominaDetalleRepository
                .findDetalleConceptosVariosPorEmpleado(
                        empresaId, IDS_INCAPACIDADES,
                        anio, periodo, documento, nombres, pageable);

        Map<String, List<Object[]>> porEmpleado = rawPage.getContent().stream()
                .collect(Collectors.groupingBy(
                        row -> row[3] + "-" + toInteger(row[0]) + "-" + toInteger(row[1])
                ));

        List<ReporteIncapacidadesDTO> dtos = porEmpleado.values().stream()
                .map(filas -> {
                    Object[] primera = filas.get(0);
                    Map<Long, BigDecimal> vals      = sumarValoresPorConcepto(filas, 6, 8);
                    Map<Long, BigDecimal> cantidades = sumarValoresPorConcepto(filas, 6, 7);

                    // pagoPor viene de observaciones de la novedad
                    String pagoPor = filas.stream()
                            .map(f -> f[9] != null ? f[9].toString() : null)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);

                    return ReporteIncapacidadesDTO.builder()
                            .anio(toInteger(primera[0]))
                            .periodo(toInteger(primera[1]))
                            .fechaPeriodo(toLocalDate(primera[2]))
                            .documentoEmp((String) primera[3])
                            .nombresEmp((String) primera[4])
                            .apellidosEmp((String) primera[5])
                            .pagoPor(pagoPor)
                            .diasIncapacidadComun(cantidades.getOrDefault(
                                    ConceptoNominaIds.INCAPACIDAD_COMUN,
                                    BigDecimal.ZERO).intValue())
                            .diasIncapacidadLaboral(cantidades.getOrDefault(
                                    ConceptoNominaIds.INCAPACIDAD_LABORAL,
                                    BigDecimal.ZERO).intValue())
                            .totalIncapacidadComun(vals.getOrDefault(
                                    ConceptoNominaIds.INCAPACIDAD_COMUN, BigDecimal.ZERO))
                            .totalIncapacidadLaboral(vals.getOrDefault(
                                    ConceptoNominaIds.INCAPACIDAD_LABORAL, BigDecimal.ZERO))
                            .build();
                })
                .sorted(Comparator.comparing(ReporteIncapacidadesDTO::getApellidosEmp))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, rawPage.getTotalElements());
    }

    public Page<ReporteIncapacidadesTotalDTO> getIncapacidadesTotalEmpresa(
            Long empresaId,
            Integer anio,
            Integer periodo,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V15 - Incapacidades total empresa={}", empresaId);

        List<Object[]> rawData = reporteNominaDetalleRepository
                .findTotalesPorConceptoIdsYPeriodo(
                        empresaId, IDS_INCAPACIDADES, anio, periodo);

        Map<String, Map<Long, BigDecimal>> porPeriodo =
                agruparPorPeriodoYConcepto(rawData);

        List<ReporteIncapacidadesTotalDTO> resultado = porPeriodo.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("-");
                    Map<Long, BigDecimal> vals = entry.getValue();

                    return ReporteIncapacidadesTotalDTO.builder()
                            .anio(Integer.parseInt(parts[0]))
                            .periodo(Integer.parseInt(parts[1]))
                            .fechaPeriodo(null)
                            .totalIncapacidadComun(vals.getOrDefault(
                                    ConceptoNominaIds.INCAPACIDAD_COMUN, BigDecimal.ZERO))
                            .totalIncapacidadLaboral(vals.getOrDefault(
                                    ConceptoNominaIds.INCAPACIDAD_LABORAL, BigDecimal.ZERO))
                            .build();
                })
                .sorted(Comparator.comparingInt(ReporteIncapacidadesTotalDTO::getAnio).reversed()
                        .thenComparingInt(ReporteIncapacidadesTotalDTO::getPeriodo).reversed())
                .collect(Collectors.toList());

        return paginar(resultado, pageable);
    }

    public Page<ReporteLicenciasDTO> getLicenciasPorEmpleado(
            Long empresaId,
            Integer anio,
            Integer periodo,
            String documento,
            String nombres,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V16 - Licencias empresa={}", empresaId);

        Page<Object[]> rawPage = reporteNominaDetalleRepository
                .findDetalleConceptosVariosPorEmpleado(
                        empresaId, IDS_LICENCIAS,
                        anio, periodo, documento, nombres, pageable);

        Map<String, List<Object[]>> porEmpleado = rawPage.getContent().stream()
                .collect(Collectors.groupingBy(
                        row -> row[3] + "-" + toInteger(row[0]) + "-" + toInteger(row[1])
                ));

        List<ReporteLicenciasDTO> dtos = porEmpleado.values().stream()
                .map(filas -> {
                    Object[] primera = filas.get(0);
                    Map<Long, BigDecimal> vals      = sumarValoresPorConcepto(filas, 6, 8);
                    Map<Long, BigDecimal> cantidades = sumarValoresPorConcepto(filas, 6, 7);

                    return ReporteLicenciasDTO.builder()
                            .anio(toInteger(primera[0]))
                            .periodo(toInteger(primera[1]))
                            .fechaPeriodo(toLocalDate(primera[2]))
                            .documentoEmp((String) primera[3])
                            .nombresEmp((String) primera[4])
                            .apellidosEmp((String) primera[5])
                            .diasLicenciaMaternidadPaternidad(
                                    cantidades.getOrDefault(ConceptoNominaIds.LICENCIA_MATERNIDAD,
                                            BigDecimal.ZERO).intValue()
                                            + cantidades.getOrDefault(ConceptoNominaIds.LICENCIA_PATERNIDAD,
                                            BigDecimal.ZERO).intValue())
                            .diasLicenciaCalamidad(cantidades.getOrDefault(
                                    ConceptoNominaIds.LICENCIA_CALAMIDAD,
                                    BigDecimal.ZERO).intValue())
                            .diasLicenciaMatrimonio(cantidades.getOrDefault(
                                    ConceptoNominaIds.LICENCIA_MATRIMONIO,
                                    BigDecimal.ZERO).intValue())
                            .diasLicenciaIsaac(cantidades.getOrDefault(
                                    ConceptoNominaIds.LICENCIA_ISAAC,
                                    BigDecimal.ZERO).intValue())
                            .diasLicenciaSufragio(cantidades.getOrDefault(
                                    ConceptoNominaIds.LICENCIA_SUFRAGIO,
                                    BigDecimal.ZERO).intValue())
                            .diasCargosTransitorios(cantidades.getOrDefault(
                                    ConceptoNominaIds.CARGOS_TRANSITORIOS,
                                    BigDecimal.ZERO).intValue())
                            .diasCitacionesJudiciales(cantidades.getOrDefault(
                                    ConceptoNominaIds.CITACIONES_JUDICIALES,
                                    BigDecimal.ZERO).intValue())
                            .diasOtrosPermisosRemunerados(cantidades.getOrDefault(
                                    ConceptoNominaIds.OTROS_PERMISOS_REMUNERADOS,
                                    BigDecimal.ZERO).intValue())
                            .diasLicenciasNoRemuneradas(cantidades.getOrDefault(
                                    ConceptoNominaIds.LICENCIAS_NO_REMUNERADAS,
                                    BigDecimal.ZERO).intValue())
                            .valorLicenciaMaternidadPaternidad(
                                    vals.getOrDefault(ConceptoNominaIds.LICENCIA_MATERNIDAD,
                                                    BigDecimal.ZERO)
                                            .add(vals.getOrDefault(ConceptoNominaIds.LICENCIA_PATERNIDAD,
                                                    BigDecimal.ZERO)))
                            .valorLicenciaCalamidad(vals.getOrDefault(
                                    ConceptoNominaIds.LICENCIA_CALAMIDAD, BigDecimal.ZERO))
                            .valorLicenciaMatrimonio(vals.getOrDefault(
                                    ConceptoNominaIds.LICENCIA_MATRIMONIO, BigDecimal.ZERO))
                            .valorLicenciaIsaac(vals.getOrDefault(
                                    ConceptoNominaIds.LICENCIA_ISAAC, BigDecimal.ZERO))
                            .valorLicenciaSufragio(vals.getOrDefault(
                                    ConceptoNominaIds.LICENCIA_SUFRAGIO, BigDecimal.ZERO))
                            .valorCargosTransitorios(vals.getOrDefault(
                                    ConceptoNominaIds.CARGOS_TRANSITORIOS, BigDecimal.ZERO))
                            .valorCitacionesJudiciales(vals.getOrDefault(
                                    ConceptoNominaIds.CITACIONES_JUDICIALES, BigDecimal.ZERO))
                            .valorOtrosPermisosRemunerados(vals.getOrDefault(
                                    ConceptoNominaIds.OTROS_PERMISOS_REMUNERADOS, BigDecimal.ZERO))
                            .valorLicenciasNoRemuneradas(vals.getOrDefault(
                                    ConceptoNominaIds.LICENCIAS_NO_REMUNERADAS, BigDecimal.ZERO))
                            .build();
                })
                .sorted(Comparator.comparing(ReporteLicenciasDTO::getApellidosEmp))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, rawPage.getTotalElements());
    }

    public Page<ReporteLicenciasTotalDTO> getLicenciasTotalEmpresa(
            Long empresaId,
            Integer anio,
            Integer periodo,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V17 - Licencias total empresa={}", empresaId);

        List<Object[]> rawData = reporteNominaDetalleRepository
                .findTotalesPorConceptoIdsYPeriodo(
                        empresaId, IDS_LICENCIAS, anio, periodo);

        Map<String, Map<Long, BigDecimal>> porPeriodo =
                agruparPorPeriodoYConcepto(rawData);

        List<ReporteLicenciasTotalDTO> resultado = porPeriodo.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("-");
                    Map<Long, BigDecimal> vals = entry.getValue();

                    BigDecimal totalRemuneradas = IDS_LICENCIAS.stream()
                            .filter(id -> !id.equals(ConceptoNominaIds.LICENCIAS_NO_REMUNERADAS))
                            .map(id -> vals.getOrDefault(id, BigDecimal.ZERO))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return ReporteLicenciasTotalDTO.builder()
                            .anio(Integer.parseInt(parts[0]))
                            .periodo(Integer.parseInt(parts[1]))
                            .fechaPeriodo(null)
                            .totalOtrosPermisosRemunerados(totalRemuneradas)
                            .totalLicenciasNoRemuneradas(vals.getOrDefault(
                                    ConceptoNominaIds.LICENCIAS_NO_REMUNERADAS, BigDecimal.ZERO))
                            .build();
                })
                .sorted(Comparator.comparingInt(ReporteLicenciasTotalDTO::getAnio).reversed()
                        .thenComparingInt(ReporteLicenciasTotalDTO::getPeriodo).reversed())
                .collect(Collectors.toList());

        return paginar(resultado, pageable);
    }

    public Page<ReporteRetefuenteDTO> getRetefuentePorEmpleado(
            Long empresaId,
            Integer anio,
            Integer periodo,
            String documento,
            String nombres,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V18 - Retefuente empresa={}", empresaId);

        Page<Object[]> rawPage = reporteNominaDetalleRepository
                .findRetefuentePorEmpleado(
                        empresaId, ConceptoNominaIds.RETEFUENTE,
                        anio, periodo, documento, nombres, pageable);

        // [0] anio  [1] periodo  [2] documento  [3] nombres
        // [4] apellidos  [5] total_retefuente
        List<ReporteRetefuenteDTO> dtos = rawPage.getContent().stream()
                .map(row -> ReporteRetefuenteDTO.builder()
                        .anio(toInteger(row[0]))
                        .periodo(toInteger(row[1]))
                        .documentoEmp((String) row[2])
                        .nombresEmp((String) row[3])
                        .apellidosEmp((String) row[4])
                        .totalRetefuente(toBigDecimal(row[5]))
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, rawPage.getTotalElements());
    }

    public Page<ReporteVacacionesEmpresaDTO> getVacacionesPorEmpresa(
            Long empresaId,
            Integer anio,
            Integer periodo,
            String documento,
            String nombres,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V19 - Vacaciones empresa={}", empresaId);

        Page<Object[]> rawPage = novedadRepository
                .findVacacionesPorEmpresa(
                        empresaId, IDS_VACACIONES,
                        anio, periodo, documento, nombres, pageable);

        // [0] novedad_id  [1] documento  [2] nombres  [3] apellidos
        // [4] fecha_inicio_ausen  [5] fecha_fin_ausen  [6] tipo_vacacion
        // [7] cantidad_dias_novedad  [8] observaciones  [9] valor_result_concept
        List<ReporteVacacionesEmpresaDTO> dtos = rawPage.getContent().stream()
                .map(row -> ReporteVacacionesEmpresaDTO.builder()
                        .anio(toInteger(row[1]))
                        .periodo(toInteger(row[2]))
                        .documentoEmp((String) row[3])
                        .nombresEmp((String) row[4])
                        .apellidosEmp((String) row[5])
                        .fechaInicioVac(toLocalDate(row[7]))
                        .fechaFinVac(toLocalDate(row[8]))
                        .tipoVacaciones(row[9] != null ? String.valueOf(row[9]).trim() : null)
                        .diasTomados(toInteger(row[10]))
                        .estadoVacaciones(row[11] != null ? row[11].toString() : null)
                        .valorPagoVac(toBigDecimal(row[12]))
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, rawPage.getTotalElements());
    }

    public Page<ReporteVacacionesTotalDTO> getVacacionesTotalEmpresa(
            Long empresaId,
            Integer anio,
            Integer periodo,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V20 - Vacaciones total empresa={}", empresaId);

        List<Object[]> rawData = reporteNominaDetalleRepository
                .findTotalesPorConceptoIdsYPeriodo(
                        empresaId, IDS_VACACIONES, anio, periodo);

        Map<String, Map<Long, BigDecimal>> porPeriodo =
                agruparPorPeriodoYConcepto(rawData);

        List<ReporteVacacionesTotalDTO> resultado = porPeriodo.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("-");
                    Map<Long, BigDecimal> vals = entry.getValue();

                    BigDecimal disfrutadas = vals.getOrDefault(
                            ConceptoNominaIds.VACACIONES_DISFRUTADAS, BigDecimal.ZERO);
                    BigDecimal compensadas = vals.getOrDefault(
                            ConceptoNominaIds.VACACIONES_COMPENSADAS, BigDecimal.ZERO);

                    return ReporteVacacionesTotalDTO.builder()
                            .anio(Integer.parseInt(parts[0]))
                            .periodo(Integer.parseInt(parts[1]))
                            .fechaPeriodo(null)
                            .totalVacacionesDisfrutadas(disfrutadas)
                            .totalVacacionesCompensadas(compensadas)
                            .build();
                })
                .sorted(Comparator.comparingInt(ReporteVacacionesTotalDTO::getAnio).reversed()
                        .thenComparingInt(ReporteVacacionesTotalDTO::getPeriodo).reversed())
                .collect(Collectors.toList());

        return paginar(resultado, pageable);
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

    private Map<Long, BigDecimal> sumarValoresPorConcepto(
            List<Object[]> filas, int indexConceptoId, int indexValor) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : filas) {
            Long conceptoId = toLong(row[indexConceptoId]);
            BigDecimal val  = toBigDecimal(row[indexValor]);
            result.merge(conceptoId, val, BigDecimal::add);
        }
        return result;
    }

    private Map<String, Map<Long, BigDecimal>> agruparPorPeriodoYConcepto(
            List<Object[]> rawData) {
        Map<String, Map<Long, BigDecimal>> result = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            String key      = toInteger(row[0]) + "-" + toInteger(row[1]);
            Long conceptoId = toLong(row[2]);
            BigDecimal val  = toBigDecimal(row[3]);
            result.computeIfAbsent(key, k -> new LinkedHashMap<>())
                    .merge(conceptoId, val, BigDecimal::add);
        }
        return result;
    }

    private <T> Page<T> paginar(List<T> lista, Pageable pageable) {
        int inicio = (int) pageable.getOffset();
        int fin    = Math.min(inicio + pageable.getPageSize(), lista.size());
        if (inicio > lista.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, lista.size());
        }
        return new PageImpl<>(lista.subList(inicio, fin), pageable, lista.size());
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
