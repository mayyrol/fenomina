package com.fenomina.master_data_service.entity;

import com.fenomina.master_data_service.enums.ParametroNombre;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "parametro_general", schema = "master_data")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParametroGeneral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "param_general_id")
    private Long paramGeneralId;

    @Enumerated(EnumType.STRING)
    @Column(name = "nombre_param_general", nullable = false)
    private ParametroNombre nombreParamGeneral;

    @Column(name = "descripcion_param", columnDefinition = "TEXT")
    private String descripcionParam;

    @Column(name = "fecha_param_general", nullable = false)
    private LocalDate fechaParamGeneral;

    @Column(name = "valor_param_general", precision = 15, scale = 3)
    private BigDecimal valorParamGeneral;

    @Column(name = "porcentaje_param_general", precision = 5, scale = 4)
    private BigDecimal porcentajeParamGeneral;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @PrePersist
    @PreUpdate
    private void validateValorOPorcentaje() {
        boolean tieneValor = valorParamGeneral != null;
        boolean tienePorcentaje = porcentajeParamGeneral != null;

        if ((tieneValor && tienePorcentaje) || (!tieneValor && !tienePorcentaje)) {
            throw new IllegalStateException(
                    "Debe tener exactamente uno de los siguientes: valor_param_general o porcentaje_param_general"
            );
        }
    }
}
