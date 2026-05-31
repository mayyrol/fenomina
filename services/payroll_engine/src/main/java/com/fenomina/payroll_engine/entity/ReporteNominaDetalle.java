package com.fenomina.payroll_engine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reporte_nomina_detalle", schema = "payroll")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteNominaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nomina_detalle_id")
    private Long nominaDetalleId;

    @Column(name = "fk_concep_nomina_id", nullable = false)
    private Long fkConcepNominaId;

    @Column(name = "fk_cabec_nomina_id", nullable = false)
    private Long fkCabecNominaId;

    @Column(name = "fk_novedad_id")
    private Long fkNovedadId;

    @Column(name = "fk_contrato_concep_id")
    private Long fkContratoConceptId;

    @Column(name = "cantidad_concept", precision = 5, scale = 2)
    private BigDecimal cantidadConcept;

    @Column(name = "base_calculo_concept", precision = 12, scale = 2)
    private BigDecimal baseCalculoConcept;

    @Column(name = "valor_result_concept", precision = 12, scale = 2)
    private BigDecimal valorResultConcept;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "observacion_concept", columnDefinition = "TEXT")
    private String observacionConcept;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
