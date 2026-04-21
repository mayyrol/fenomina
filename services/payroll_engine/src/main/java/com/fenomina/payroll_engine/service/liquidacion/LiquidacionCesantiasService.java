package com.fenomina.payroll_engine.service.liquidacion;

import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;

import java.util.List;

public interface LiquidacionCesantiasService {
    void liquidar(ProcesoLiquidacion proceso, List<Long> empleadosSeleccionados);
}