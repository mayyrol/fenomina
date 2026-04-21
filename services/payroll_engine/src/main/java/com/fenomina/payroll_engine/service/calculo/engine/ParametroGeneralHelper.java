package com.fenomina.payroll_engine.service.calculo.engine;

import com.fenomina.payroll_engine.exception.ParametroNoEncontradoException;
import com.fenomina.payroll_engine.client.dto.ParametroGeneralDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Component
public class ParametroGeneralHelper {

    public BigDecimal getValor(
            List<ParametroGeneralDTO> parametros,
            String nombre,
            LocalDate fechaPeriodo
    ) {
        return parametros.stream()
                .filter(p -> p.nombreParamGeneral().equals(nombre))
                .filter(p -> !p.fechaParamGeneral().isAfter(fechaPeriodo))
                .max(Comparator.comparing(ParametroGeneralDTO::fechaParamGeneral))
                .map(ParametroGeneralDTO::valorParamGeneral)
                .orElseThrow(() -> new ParametroNoEncontradoException(
                        String.format("Parámetro no encontrado: %s para fecha %s",
                                nombre, fechaPeriodo)
                ));
    }

    public BigDecimal getPorcentaje(
            List<ParametroGeneralDTO> parametros,
            String nombre,
            LocalDate fechaPeriodo
    ) {
        return parametros.stream()
                .filter(p -> p.nombreParamGeneral().equals(nombre))
                .filter(p -> !p.fechaParamGeneral().isAfter(fechaPeriodo))
                .max(Comparator.comparing(ParametroGeneralDTO::fechaParamGeneral))
                .map(ParametroGeneralDTO::porcentajeParamGeneral)
                .orElseThrow(() -> new ParametroNoEncontradoException(
                        String.format("Parámetro no encontrado: %s para fecha %s",
                                nombre, fechaPeriodo)
                ));
    }

    public BigDecimal getFspPorcentaje(
            List<ParametroGeneralDTO> parametros,
            BigDecimal ibcEnSMMLV,
            LocalDate fechaPeriodo
    ) {
        BigDecimal smmlv = getValor(parametros, "SMMLV", fechaPeriodo);

        if (ibcEnSMMLV.compareTo(smmlv.multiply(BigDecimal.valueOf(4))) < 0) {
            return BigDecimal.ZERO;
        }

        String nombreFsp;

        if (ibcEnSMMLV.compareTo(smmlv.multiply(BigDecimal.valueOf(16))) < 0) {
            nombreFsp = "FONDO_SOLIDARIDAD_PENSIONAL_1";
        } else if (ibcEnSMMLV.compareTo(smmlv.multiply(BigDecimal.valueOf(17))) < 0) {
            nombreFsp = "FONDO_SOLIDARIDAD_PENSIONAL_2";
        } else if (ibcEnSMMLV.compareTo(smmlv.multiply(BigDecimal.valueOf(18))) < 0) {
            nombreFsp = "FONDO_SOLIDARIDAD_PENSIONAL_3";
        } else if (ibcEnSMMLV.compareTo(smmlv.multiply(BigDecimal.valueOf(19))) < 0) {
            nombreFsp = "FONDO_SOLIDARIDAD_PENSIONAL_4";
        } else if (ibcEnSMMLV.compareTo(smmlv.multiply(BigDecimal.valueOf(20))) < 0) {
            nombreFsp = "FONDO_SOLIDARIDAD_PENSIONAL_5";
        } else {
            nombreFsp = "FONDO_SOLIDARIDAD_PENSIONAL_6";
        }

        return getPorcentaje(parametros, nombreFsp, fechaPeriodo);
    }

    public BigDecimal getArlPorcentaje(
            List<ParametroGeneralDTO> parametros,
            String claseRiesgo,
            LocalDate fechaPeriodo
    ) {
        String nombreParam = switch (claseRiesgo.toUpperCase()) {
            case "I", "1" -> "ARL_EMPLEADOR_I";
            case "II", "2" -> "ARL_EMPLEADOR_II";
            case "III", "3" -> "ARL_EMPLEADOR_III";
            case "IV", "4" -> "ARL_EMPLEADOR_IV";
            case "V", "5" -> "ARL_EMPLEADOR_V";
            default -> throw new ParametroNoEncontradoException(
                    String.format("Clase de riesgo ARL no reconocida: %s", claseRiesgo)
            );
        };

        return getPorcentaje(parametros, nombreParam, fechaPeriodo);
    }
}
