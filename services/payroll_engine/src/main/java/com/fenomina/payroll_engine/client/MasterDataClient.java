package com.fenomina.payroll_engine.client;

import com.fenomina.payroll_engine.client.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "master-data-service",
        url = "${services.master-data.url}"
)
public interface MasterDataClient {

    @GetMapping("/api/master/empleados")
    List<EmpleadoDTO> findEmpleadosActivos(
            @RequestParam("empresaId") Long empresaId,
            @RequestParam("estado") String estado
    );

    @GetMapping("/api/master/parametros")
    List<ParametroGeneralDTO> findAllParametros();

    @GetMapping("/api/master/empleados/{empleadoId}/conceptos")
    List<ContratoConceptoDTO> findConceptosFijosByEmpleado(
            @PathVariable("empleadoId") Long empleadoId
    );

    @GetMapping("/api/master/internal/conceptos-nomina")
    List<ConceptoNominaDTO> findAllConceptosNomina(
            @RequestHeader("X-Internal-Api-Key") String internalApiKey
    );

    @GetMapping("/api/master/empresas/{empresaId}")
    EmpresaDTO findEmpresaById(@PathVariable("empresaId") Long empresaId);
}
