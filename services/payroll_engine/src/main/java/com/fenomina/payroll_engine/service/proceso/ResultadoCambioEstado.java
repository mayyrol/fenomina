package com.fenomina.payroll_engine.service.proceso;

import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import java.util.List;

public record ResultadoCambioEstado(
        ProcesoLiquidacion proceso,
        List<String> advertencias
) {}
