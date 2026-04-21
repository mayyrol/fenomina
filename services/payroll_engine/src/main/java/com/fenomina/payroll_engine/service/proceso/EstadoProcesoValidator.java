package com.fenomina.payroll_engine.service.proceso;

import com.fenomina.payroll_engine.enums.EstadoProceso;
import com.fenomina.payroll_engine.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EstadoProcesoValidator {

    private static final Map<EstadoProceso, List<EstadoProceso>> TRANSICIONES_PERMITIDAS =
            Map.of(
                    EstadoProceso.BORRADOR, List.of(
                            EstadoProceso.CERRADO,
                            EstadoProceso.ANULADO
                    ),
                    EstadoProceso.CERRADO, List.of(
                            EstadoProceso.BORRADOR,
                            EstadoProceso.PENDIENTE_PAGO,
                            EstadoProceso.ANULADO
                    ),
                    EstadoProceso.PENDIENTE_PAGO, List.of(
                            EstadoProceso.PAGADO,
                            EstadoProceso.ANULADO
                    ),
                    EstadoProceso.PAGADO, List.of(
                            EstadoProceso.ANULADO
                    ),
                    EstadoProceso.ANULADO, List.of()
            );

    public void validarTransicion(EstadoProceso estadoActual, EstadoProceso estadoNuevo) {
        List<EstadoProceso> permitidos = TRANSICIONES_PERMITIDAS.get(estadoActual);

        if (permitidos == null || !permitidos.contains(estadoNuevo)) {
            throw new InvalidStateTransitionException(
                    String.format("Transición no permitida: %s → %s", estadoActual, estadoNuevo)
            );
        }
    }
}
