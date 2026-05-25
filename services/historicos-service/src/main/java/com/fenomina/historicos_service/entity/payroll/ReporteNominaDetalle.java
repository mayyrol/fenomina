package com.fenomina.historicos_service.entity.payroll;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "reporte_nomina_detalle", schema = "payroll")
public class ReporteNominaDetalle {

    @Id
    @Column(name = "nomina_detalle_id")
    private Long nominaDetalleId;

    @Column(name = "fk_concep_nomina_id")
    private Long fkConcepNominaId;

    @Column(name = "fk_cabec_nomina_id")
    private Long fkCabecNominaId;

    @Column(name = "fk_novedad_id")
    private Long fkNovedadId;

    @Column(name = "fk_contrato_concep_id")
    private Long fkContratoConcepId;

    @Column(name = "cantidad_concept")
    private BigDecimal cantidadConcept;

    @Column(name = "base_calculo_concept")
    private BigDecimal baseCalculoConcept;

    @Column(name = "valor_result_concept")
    private BigDecimal valorResultConcept;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}