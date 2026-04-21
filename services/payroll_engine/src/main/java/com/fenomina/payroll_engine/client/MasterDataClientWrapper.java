package com.fenomina.payroll_engine.client;

import com.fenomina.payroll_engine.client.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MasterDataClientWrapper {

    private static final String ESTADO_ACTIVO = "ACTIVO";

    @Value("${internal.api.key}")
    private String internalApiKey;

    private final MasterDataClient masterDataClient;

    public List<EmpleadoDTO> findEmpleadosActivos(Long empresaId) {
        return masterDataClient.findEmpleadosActivos(empresaId, ESTADO_ACTIVO);
    }

    public List<ParametroGeneralDTO> findAllParametros() {
        return masterDataClient.findAllParametros();
    }

    public List<ContratoConceptoDTO> findConceptosFijosByEmpleado(Long empleadoId) {
        return masterDataClient.findConceptosFijosByEmpleado(empleadoId);
    }

    public List<ConceptoNominaDTO> findAllConceptosNomina() {
        return masterDataClient.findAllConceptosNomina(internalApiKey);
    }

    public EmpresaDTO findEmpresaById(Long empresaId) {
        return masterDataClient.findEmpresaById(empresaId);
    }
}
