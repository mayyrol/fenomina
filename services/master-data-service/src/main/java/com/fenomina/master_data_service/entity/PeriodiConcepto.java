package com.fenomina.master_data_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "periodi_concepto", schema = "master_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodiConcepto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "periodi_concepto_id")
    private Long periodiConceptoId;

    @Column(name = "nombre_periodi", nullable = false, unique = true, length = 30)
    private String nombrePeriodi;

    @Column(name = "valor_periodi", nullable = false)
    private Integer valorPeriodi;
}
