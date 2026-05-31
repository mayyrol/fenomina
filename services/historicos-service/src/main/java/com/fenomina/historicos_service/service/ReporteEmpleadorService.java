package com.fenomina.historicos_service.service;

import com.fenomina.historicos_service.config.ConceptoNominaIds;
import com.fenomina.historicos_service.dto.empleador.*;
import com.fenomina.historicos_service.exception.AccesoNoAutorizadoException;
import com.fenomina.historicos_service.repository.payroll.DetalleLiquiPrestacionRepository;
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
public class ReporteEmpleadorService {

    private final ReporteNominaDetalleRepository reporteNominaDetalleRepository;
    private final DetalleLiquiPrestacionRepository detalleLiquiPrestacionRepository;

    private static final List<Long> IDS_SEG_SOCIAL = List.of(
            ConceptoNominaIds.SALUD_EMPLEADOR,
            ConceptoNominaIds.PENSION_EMPLEADOR,
            ConceptoNominaIds.ARL_EMPLEADOR
    );

    private static final List<Long> IDS_PARAFISCALES = List.of(
            ConceptoNominaIds.SENA_EMPLEADOR,
            ConceptoNominaIds.ICBF_EMPLEADOR,
            ConceptoNominaIds.CAJA_COMP
    );

    private static final List<Long> IDS_TODOS_PATRONALES = List.of(
            ConceptoNominaIds.SALUD_EMPLEADOR,
            ConceptoNominaIds.PENSION_EMPLEADOR,
            ConceptoNominaIds.ARL_EMPLEADOR,
            ConceptoNominaIds.SENA_EMPLEADOR,
            ConceptoNominaIds.ICBF_EMPLEADOR,
            ConceptoNominaIds.CAJA_COMP
    );

    public Page<ReporteSegSocialTotalDTO> getSegSocialTotal(
            Long empresaId,
            Integer anio,
            Integer periodo,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V7 - Seg social total empresa={}, anio={}, periodo={}",
                empresaId, anio, periodo);

        List<Object[]> rawData = reporteNominaDetalleRepository
                .findTotalesPorConceptoIdsYPeriodo(
                        empresaId, IDS_SEG_SOCIAL, anio, periodo);

        // [0] anio  [1] periodo  [2] fk_concep_nomina_id  [3] total_valor
        Map<String, Map<Long, BigDecimal>> porPeriodo =
                agruparPorPeriodoYConcepto(rawData);

        List<ReporteSegSocialTotalDTO> resultado = porPeriodo.entrySet().stream()
                .map(entry -> {
                    String[] parts  = entry.getKey().split("-");
                    Map<Long, BigDecimal> vals = entry.getValue();

                    BigDecimal salud   = vals.getOrDefault(
                            ConceptoNominaIds.SALUD_EMPLEADOR, BigDecimal.ZERO);
                    BigDecimal pension = vals.getOrDefault(
                            ConceptoNominaIds.PENSION_EMPLEADOR, BigDecimal.ZERO);
                    BigDecimal arl     = vals.getOrDefault(
                            ConceptoNominaIds.ARL_EMPLEADOR, BigDecimal.ZERO);

                    return ReporteSegSocialTotalDTO.builder()
                            .anio(Integer.parseInt(parts[0]))
                            .periodo(Integer.parseInt(parts[1]))
                            .segSocialSalud(salud)
                            .segSocialPension(pension)
                            .segSocialArl(arl)
                            .totalSegSocialEmpr(salud.add(pension).add(arl))
                            .build();
                })
                .sorted(Comparator
                        .comparingInt(ReporteSegSocialTotalDTO::getAnio).reversed()
                        .thenComparingInt(ReporteSegSocialTotalDTO::getPeriodo).reversed())
                .collect(Collectors.toList());

        return paginar(resultado, pageable);
    }

    public Page<ReporteSegSocialXEmpleadoDTO> getSegSocialXEmpleado(
            Long empresaId,
            Integer anio,
            Integer periodo,
            String documento,
            String nombres,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V7.1 - Seg social x empleado empresa={}", empresaId);

        List<Object[]> rawList = reporteNominaDetalleRepository
                .findDetallesPorConceptoIdsYEmpleadoSinPaginar(
                        empresaId, IDS_SEG_SOCIAL,
                        anio, periodo, documento, nombres);

        Map<String, List<Object[]>> porEmpleado = rawList.stream()
                .collect(Collectors.groupingBy(
                        row -> row[2] + "-" + toInteger(row[0]) + "-" + toInteger(row[1])
                ));

        List<ReporteSegSocialXEmpleadoDTO> dtos = porEmpleado.values().stream()
                .map(filas -> {
                    Object[] primera = filas.get(0);
                    Map<Long, BigDecimal> vals = sumarPorConceptoId(filas, 6, 7);

                    BigDecimal salud   = vals.getOrDefault(
                            ConceptoNominaIds.SALUD_EMPLEADOR, BigDecimal.ZERO);
                    BigDecimal pension = vals.getOrDefault(
                            ConceptoNominaIds.PENSION_EMPLEADOR, BigDecimal.ZERO);
                    BigDecimal arl     = vals.getOrDefault(
                            ConceptoNominaIds.ARL_EMPLEADOR, BigDecimal.ZERO);

                    return ReporteSegSocialXEmpleadoDTO.builder()
                            .anio(toInteger(primera[0]))
                            .periodo(toInteger(primera[1]))
                            .documentoEmp((String) primera[2])
                            .nombresEmp((String) primera[3])
                            .apellidosEmp((String) primera[4])
                            .fechaIngresoEmp(toLocalDate(primera[5]))
                            .empleadorSalud(salud)
                            .empleadorPension(pension)
                            .empleadorArl(arl)
                            .totalSocialEmpleador(salud.add(pension).add(arl))
                            .build();
                })
                .sorted(Comparator.comparing(ReporteSegSocialXEmpleadoDTO::getApellidosEmp))
                .collect(Collectors.toList());

        return paginar(dtos, pageable);
    }

    public Page<ReporteAportesParafTotalDTO> getAportesParafTotal(
            Long empresaId,
            Integer anio,
            Integer periodo,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V8 - Aportes parafiscales total empresa={}", empresaId);

        List<Object[]> rawData = reporteNominaDetalleRepository
                .findTotalesPorConceptoIdsYPeriodo(
                        empresaId, IDS_PARAFISCALES, anio, periodo);

        Map<String, Map<Long, BigDecimal>> porPeriodo =
                agruparPorPeriodoYConcepto(rawData);

        List<ReporteAportesParafTotalDTO> resultado = porPeriodo.entrySet().stream()
                .map(entry -> {
                    String[] parts  = entry.getKey().split("-");
                    Map<Long, BigDecimal> vals = entry.getValue();

                    BigDecimal sena = vals.getOrDefault(
                            ConceptoNominaIds.SENA_EMPLEADOR, BigDecimal.ZERO);
                    BigDecimal icbf = vals.getOrDefault(
                            ConceptoNominaIds.ICBF_EMPLEADOR, BigDecimal.ZERO);
                    BigDecimal caja = vals.getOrDefault(
                            ConceptoNominaIds.CAJA_COMP, BigDecimal.ZERO);

                    return ReporteAportesParafTotalDTO.builder()
                            .anio(Integer.parseInt(parts[0]))
                            .periodo(Integer.parseInt(parts[1]))
                            .apFiscaSena(sena)
                            .apFiscaIcbf(icbf)
                            .apFiscaCajaComp(caja)
                            .totalAportesParafEmpr(sena.add(icbf).add(caja))
                            .build();
                })
                .sorted(Comparator
                        .comparingInt(ReporteAportesParafTotalDTO::getAnio).reversed()
                        .thenComparingInt(ReporteAportesParafTotalDTO::getPeriodo).reversed())
                .collect(Collectors.toList());

        return paginar(resultado, pageable);
    }

    public Page<ReporteAportesParafXEmpleadoDTO> getAportesParafXEmpleado(
            Long empresaId,
            Integer anio,
            Integer periodo,
            String documento,
            String nombres,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V8.1 - Aportes parafiscales x empleado empresa={}", empresaId);

        List<Object[]> rawList = reporteNominaDetalleRepository
                .findDetallesPorConceptoIdsYEmpleadoSinPaginar(
                        empresaId, IDS_PARAFISCALES,
                        anio, periodo, documento, nombres);

        Map<String, List<Object[]>> porEmpleado = rawList.stream()
                .collect(Collectors.groupingBy(
                        row -> row[2] + "-" + toInteger(row[0]) + "-" + toInteger(row[1])
                ));

        List<ReporteAportesParafXEmpleadoDTO> dtos = porEmpleado.values().stream()
                .map(filas -> {
                    Object[] primera = filas.get(0);
                    Map<Long, BigDecimal> vals = sumarPorConceptoId(filas, 6, 7);

                    BigDecimal sena = vals.getOrDefault(
                            ConceptoNominaIds.SENA_EMPLEADOR, BigDecimal.ZERO);
                    BigDecimal icbf = vals.getOrDefault(
                            ConceptoNominaIds.ICBF_EMPLEADOR, BigDecimal.ZERO);
                    BigDecimal caja = vals.getOrDefault(
                            ConceptoNominaIds.CAJA_COMP, BigDecimal.ZERO);

                    return ReporteAportesParafXEmpleadoDTO.builder()
                            .anio(toInteger(primera[0]))
                            .periodo(toInteger(primera[1]))
                            .documentoEmp((String) primera[2])
                            .nombresEmp((String) primera[3])
                            .apellidosEmp((String) primera[4])
                            .fechaIngresoEmp(toLocalDate(primera[5]))
                            .apFiscaSena(sena)
                            .apFiscaIcbf(icbf)
                            .apFiscaCajaComp(caja)
                            .totalAportesParafEmpleador(sena.add(icbf).add(caja))
                            .build();
                })
                .sorted(Comparator.comparing(ReporteAportesParafXEmpleadoDTO::getApellidosEmp))
                .collect(Collectors.toList());

        return paginar(dtos, pageable);
    }

    public Page<ReporteCargasPrestTotalDTO> getCargasPrestacionalesTotal(
            Long empresaId,
            Integer anio,
            Integer periodo,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V9 - Cargas prestacionales empresa={}", empresaId);

        Page<Object[]> cesantiasPage = detalleLiquiPrestacionRepository
                .findTotalesPrestacionesPorEmpresaYPeriodo(
                        empresaId, "CESANTIAS_ANUAL", anio, periodo, Pageable.unpaged());

        Page<Object[]> primasPage = detalleLiquiPrestacionRepository
                .findTotalesPrestacionesPorEmpresaYPeriodo(
                        empresaId, "PRIMA_SEMESTRAL", anio, periodo, Pageable.unpaged());

        Map<Integer, Object[]> cesantiasPorAnio = cesantiasPage.getContent().stream()
                .collect(Collectors.toMap(
                        row -> toInteger(row[0]),
                        row -> row,
                        (a, b) -> a
                ));
        Map<Integer, List<Object[]>> primasPorAnio = primasPage.getContent().stream()
                .collect(Collectors.groupingBy(row -> toInteger(row[0])));

        Set<Integer> aniosUnicos = new LinkedHashSet<>();
        cesantiasPage.getContent().forEach(r -> aniosUnicos.add(toInteger(r[0])));
        primasPage.getContent().forEach(r -> aniosUnicos.add(toInteger(r[0])));

        List<ReporteCargasPrestTotalDTO> resultado = new ArrayList<>();

        for (Integer anioVal : aniosUnicos) {
            Object[] rowCes = cesantiasPorAnio.get(anioVal);
            List<Object[]> primasAnio = primasPorAnio.getOrDefault(
                    anioVal, Collections.emptyList());

            if (primasAnio.isEmpty()) {
                resultado.add(ReporteCargasPrestTotalDTO.builder()
                        .anio(anioVal)
                        .periodo(rowCes != null ? toInteger(rowCes[1]) : null)
                        .cargPresCesantiasInformativo(
                                rowCes != null ? toBigDecimal(rowCes[3]) : BigDecimal.ZERO)
                        .cargPresPrimas(BigDecimal.ZERO)
                        .cargPresIntCesantias(
                                rowCes != null ? toBigDecimal(rowCes[4]) : BigDecimal.ZERO)
                        .build());
            } else {
                // Una fila por cada semestre de prima
                for (Object[] rowPrima : primasAnio) {
                    resultado.add(ReporteCargasPrestTotalDTO.builder()
                            .anio(anioVal)
                            .periodo(toInteger(rowPrima[1]))
                            .cargPresCesantiasInformativo(
                                    rowCes != null ? toBigDecimal(rowCes[3]) : BigDecimal.ZERO)
                            .cargPresPrimas(toBigDecimal(rowPrima[3]))
                            .cargPresIntCesantias(
                                    rowCes != null ? toBigDecimal(rowCes[4]) : BigDecimal.ZERO)
                            .build());
                }
            }
        }
        resultado.sort(Comparator
                .comparingInt(ReporteCargasPrestTotalDTO::getAnio).reversed()
                .thenComparingInt(r -> r.getPeriodo() != null ? r.getPeriodo() : 0));
        Collections.reverse(resultado); // compensar el thenComparing
        resultado.sort((a, b) -> {
            int cmpAnio = Integer.compare(b.getAnio(), a.getAnio());
            if (cmpAnio != 0) return cmpAnio;
            int pa = a.getPeriodo() != null ? a.getPeriodo() : 0;
            int pb = b.getPeriodo() != null ? b.getPeriodo() : 0;
            return Integer.compare(pb, pa);
        });

        return paginar(resultado, pageable);
    }

    public Page<ReporteConceptosEmpleadorTotalDTO> getConceptosEmpleadorTotal(
            Long empresaId,
            Integer anio,
            Integer periodo,
            Pageable pageable) {

        validarAccesoEmpresa(empresaId);

        log.debug("V10 - Conceptos empleador total empresa={}", empresaId);

        List<Object[]> todosAportesRaw = reporteNominaDetalleRepository
                .findTotalesPorConceptoIdsYPeriodo(
                        empresaId, IDS_TODOS_PATRONALES, anio, periodo);

        Page<Object[]> cesantiasPage = detalleLiquiPrestacionRepository
                .findTotalesPrestacionesPorEmpresaYPeriodo(
                        empresaId, "CESANTIAS_ANUAL", anio, periodo, pageable);

        Page<Object[]> primasPage = detalleLiquiPrestacionRepository
                .findTotalesPrestacionesPorEmpresaYPeriodo(
                        empresaId, "PRIMA_SEMESTRAL", anio, periodo, pageable);


        Map<String, Map<Long, BigDecimal>> aportesPorPeriodo =
                agruparPorPeriodoYConcepto(todosAportesRaw);

        Map<String, Object[]> primasPorPeriodo = primasPage.getContent().stream()
                .collect(Collectors.toMap(
                        row -> toInteger(row[0]) + "-" + toInteger(row[1]),
                        row -> row,
                        (a, b) -> a
                ));

        List<ReporteConceptosEmpleadorTotalDTO> resultado =
                cesantiasPage.getContent().stream()
                        .map(rowCes -> {
                            Integer anioVal    = toInteger(rowCes[0]);
                            Integer periodoVal = toInteger(rowCes[1]);
                            String key = anioVal + "-" + periodoVal;

                            Map<Long, BigDecimal> vals = aportesPorPeriodo
                                    .getOrDefault(key, Collections.emptyMap());

                            BigDecimal salud   = vals.getOrDefault(
                                    ConceptoNominaIds.SALUD_EMPLEADOR, BigDecimal.ZERO);
                            BigDecimal pension = vals.getOrDefault(
                                    ConceptoNominaIds.PENSION_EMPLEADOR, BigDecimal.ZERO);
                            BigDecimal arl     = vals.getOrDefault(
                                    ConceptoNominaIds.ARL_EMPLEADOR, BigDecimal.ZERO);
                            BigDecimal sena    = vals.getOrDefault(
                                    ConceptoNominaIds.SENA_EMPLEADOR, BigDecimal.ZERO);
                            BigDecimal icbf    = vals.getOrDefault(
                                    ConceptoNominaIds.ICBF_EMPLEADOR, BigDecimal.ZERO);
                            BigDecimal caja    = vals.getOrDefault(
                                    ConceptoNominaIds.CAJA_COMP, BigDecimal.ZERO);

                            Object[] rowPrima = primasPorPeriodo.get(key);

                            return ReporteConceptosEmpleadorTotalDTO.builder()
                                    .anio(anioVal)
                                    .periodo(periodoVal)
                                    .totalSegSocialEmpr(salud.add(pension).add(arl))
                                    .totalAportesParafEmpr(sena.add(icbf).add(caja))
                                    .cargPresPrimas(rowPrima != null
                                            ? toBigDecimal(rowPrima[3]) : BigDecimal.ZERO)
                                    .cargPresIntCesantias(toBigDecimal(rowCes[4]))
                                    .build();
                        })
                        .collect(Collectors.toList());

        return new PageImpl<>(resultado, pageable, cesantiasPage.getTotalElements());
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

    private Map<String, Map<Long, BigDecimal>> agruparPorPeriodoYConcepto(
            List<Object[]> rawData) {
        // [0] anio  [1] periodo  [2] fk_concep_nomina_id  [3] total_valor
        Map<String, Map<Long, BigDecimal>> result = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            String key     = toInteger(row[0]) + "-" + toInteger(row[1]);
            Long conceptoId = toLong(row[2]);
            BigDecimal val  = toBigDecimal(row[3]);
            result.computeIfAbsent(key, k -> new LinkedHashMap<>())
                    .merge(conceptoId, val, BigDecimal::add);
        }
        return result;
    }

    private Map<Long, BigDecimal> sumarPorConceptoId(
            List<Object[]> filas, int indexConceptoId, int indexValor) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : filas) {
            Long conceptoId = toLong(row[indexConceptoId]);
            BigDecimal val  = toBigDecimal(row[indexValor]);
            result.merge(conceptoId, val, BigDecimal::add);
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
