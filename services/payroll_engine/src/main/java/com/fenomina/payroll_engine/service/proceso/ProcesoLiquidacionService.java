package com.fenomina.payroll_engine.service.proceso;

import com.fenomina.payroll_engine.entity.ProcesoLiquidacion;
import com.fenomina.payroll_engine.enums.EstadoProceso;
import com.fenomina.payroll_engine.enums.TipoProceso;

import java.time.LocalDate;
import java.util.List;

public interface ProcesoLiquidacionService {

    ProcesoLiquidacion crear(
            Long empresaId,
            Long usuarioId,
            TipoProceso tipoProceso,
            Integer anio,
            Integer periodo,
            LocalDate fechaInicio,
            LocalDate fechaFin
    );

    ResultadoCambioEstado cambiarEstado(Long procesoId, EstadoProceso nuevoEstado, Long usuarioId);

    ProcesoLiquidacion findById(Long procesoId);

    List<ProcesoLiquidacion> findByEmpresa(Long empresaId);

    void eliminar(Long procesoId);
}
