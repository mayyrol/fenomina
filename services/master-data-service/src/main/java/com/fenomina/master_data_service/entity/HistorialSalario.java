package com.fenomina.master_data_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_salario", schema = "master_data")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialSalario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hist_salario_id")
    private Long histSalarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_empleado_id", nullable = false)
    private Empleado empleado;

    @Column(name = "salario_anterior", nullable = false, precision = 12, scale = 2)
    private BigDecimal salarioAnterior;

    @Column(name = "salario_actual", nullable = false, precision = 12, scale = 2)
    private BigDecimal salarioActual;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;
}