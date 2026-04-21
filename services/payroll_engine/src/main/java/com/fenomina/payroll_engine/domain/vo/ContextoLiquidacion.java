package com.fenomina.payroll_engine.domain.vo;

import com.fenomina.payroll_engine.entity.Novedad;
import com.fenomina.payroll_engine.client.dto.ConceptoNominaDTO;
import com.fenomina.payroll_engine.client.dto.ContratoConceptoDTO;
import com.fenomina.payroll_engine.client.dto.EmpleadoDTO;
import com.fenomina.payroll_engine.client.dto.ParametroGeneralDTO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ContextoLiquidacion {

    private final EmpleadoDTO empleado;
    private final Long procesoId;
    private final Integer anio;
    private final Integer periodo;
    private final LocalDate fechaInicioPeriodo;
    private final LocalDate fechaFinPeriodo;
    private final boolean esQuincenal;
    private final Integer diasLaborados;
    private final Integer diasLicenciaNoRemunerada;
    private final List<Novedad> novedades;
    private final List<ContratoConceptoDTO> conceptosFijos;
    private final Map<Long, ConceptoNominaDTO> conceptosPorId;
    private final Map<String, ConceptoNominaDTO> conceptosPorNombre;
    private final Map<String, ParametroGeneralDTO> parametrosPorNombre;
    private final BigDecimal ibcSaludAnterior;
    private final BigDecimal ibcPensionAnterior;
    private final Boolean esEmpresaExoneradaParafiscales;
}
