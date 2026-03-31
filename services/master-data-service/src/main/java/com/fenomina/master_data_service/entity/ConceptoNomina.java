package com.fenomina.master_data_service.entity;

import com.fenomina.master_data_service.enums.CategoriaConcepto;
import com.fenomina.master_data_service.enums.TipoEntradaConcepto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "concepto_nomina", schema = "master_data")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConceptoNomina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concep_nomina_id")
    private Long concepNominaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_periodi_concepto_id", nullable = false)
    private PeriodiConcepto periodiConcepto;

    @Column(name = "nombre_concep_nomina", nullable = false, unique = true)
    private String nombreConcepNomina;

    @Column(name = "descr_concep_nomina", columnDefinition = "TEXT")
    private String descrConcepNomina;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_conc_nomina", nullable = false, length = 50)
    private CategoriaConcepto categoriaConcNomina;

    @Column(name = "es_salario", nullable = false)
    private Boolean esSalario = false;

    @Column(name = "es_ibc", nullable = false)
    private Boolean esIbc = false;

    @Column(name = "es_informativo", nullable = false)
    private Boolean esInformativo = false;

    @Column(name = "es_variable", nullable = false)
    private Boolean esVariable = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entrada_concept", nullable = false, length = 20)
    private TipoEntradaConcepto tipoEntradaConcept;

    @Column(name = "responsable_pago_lic_inca", length = 30)
    private String responsablePagoLicInca;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
