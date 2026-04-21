package com.fenomina.payroll_engine.service.liquidacion;

import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;

public interface LiquidacionPrimaService {
    void liquidar(ProcesoLiquidacion proceso, java.util.List<Long> empleadosSeleccionados);
}
