package com.fenomina.historicos_service.entity.master;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "parametro_general", schema = "master_data")
public class ParametroGeneral {

    @Id
    @Column(name = "param_general_id")
    private Long paramGeneralId;

    @Column(name = "nombre_param_general")
    private String nombreParamGeneral;

    @Column(name = "descripcion_param")
    private String descripcionParam;

    @Column(name = "fecha_param_general")
    private LocalDate fechaParamGeneral;

    @Column(name = "valor_param_general")
    private BigDecimal valorParamGeneral;

    @Column(name = "porcentaje_param_general")
    private BigDecimal porcentajeParamGeneral;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
