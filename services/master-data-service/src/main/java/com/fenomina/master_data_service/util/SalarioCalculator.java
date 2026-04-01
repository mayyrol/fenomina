package com.fenomina.master_data_service.util;

import com.fenomina.master_data_service.entity.ParametroGeneral;
import com.fenomina.master_data_service.enums.ParametroNombre;
import com.fenomina.master_data_service.repository.ParametroGeneralRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalarioCalculator {

    private final ParametroGeneralRepository parametroGeneralRepository;

    private static final String SMMLV_PARAM_NAME = "SMMLV";
    private static final BigDecimal MULTIPLICADOR_SALARIO_INTEGRAL = BigDecimal.valueOf(13);

    public boolean esSalarioIntegral(BigDecimal salarioBasico, LocalDate fecha) {
        if (salarioBasico == null) {
            throw new IllegalArgumentException("El salario básico no puede ser nulo");
        }

        BigDecimal smmlv = obtenerSMMLVVigente(fecha);
        BigDecimal limiteIntegral = smmlv.multiply(MULTIPLICADOR_SALARIO_INTEGRAL);

        boolean esIntegral = salarioBasico.compareTo(limiteIntegral) >= 0;

        log.debug("Validación salario integral: Salario={}, SMMLV={}, Límite={}, EsIntegral={}",
                salarioBasico, smmlv, limiteIntegral, esIntegral);

        return esIntegral;
    }

    public BigDecimal obtenerSMMLVVigente(LocalDate fecha) {
        if (fecha == null) {
            fecha = LocalDate.now();
        }

        Optional<ParametroGeneral> parametroOpt = parametroGeneralRepository
                .findVigenteByNombreAndFecha(ParametroNombre.SMMLV, fecha);  // CAMBIO: usa enum

        if (parametroOpt.isEmpty()) {
            log.error("No se encontró SMMLV vigente para la fecha: {}", fecha);
            throw new IllegalStateException(
                    "No se encontró el parámetro SMMLV vigente para la fecha " + fecha +
                            ". Por favor, configure el parámetro en el sistema."
            );
        }

        ParametroGeneral parametro = parametroOpt.get();
        BigDecimal smmlv = parametro.getValorParamGeneral();

        log.debug("SMMLV vigente para {}: {} (desde {})",
                fecha, smmlv, parametro.getFechaParamGeneral());

        return smmlv;
    }

    public boolean tieneDerechoAuxilioTransporte(BigDecimal salarioBasico, LocalDate fecha) {
        if (salarioBasico == null) {
            throw new IllegalArgumentException("El salario básico no puede ser nulo");
        }

        BigDecimal smmlv = obtenerSMMLVVigente(fecha);
        BigDecimal limiteParaAuxTransporte = smmlv.multiply(BigDecimal.valueOf(2));

        boolean tieneDerecho = salarioBasico.compareTo(limiteParaAuxTransporte) <= 0;

        log.debug("Validación auxilio transporte: Salario={}, Límite={}, TieneDerecho={}",
                salarioBasico, limiteParaAuxTransporte, tieneDerecho);

        return tieneDerecho;
    }

    public BigDecimal calcularFactorSalarial(BigDecimal salarioBasico, LocalDate fecha) {
        BigDecimal smmlv = obtenerSMMLVVigente(fecha);
        return salarioBasico.divide(smmlv, 2, RoundingMode.HALF_UP);
    }
}
