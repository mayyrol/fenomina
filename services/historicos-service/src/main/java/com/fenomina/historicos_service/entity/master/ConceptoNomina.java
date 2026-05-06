package com.fenomina.historicos_service.entity.master;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "concepto_nomina", schema = "master_data")
public class ConceptoNomina {

    @Id
    @Column(name = "concep_nomina_id")
    private Long concepNominaId;

    @Column(name = "fk_periodi_concepto_id")
    private Long fkPeriodiConceptoId;

    @Column(name = "nombre_concep_nomina")
    private String nombreConcepNomina;

    @Column(name = "descr_concep_nomina")
    private String descrConcepNomina;

    @Column(name = "categoria_conc_nomina")
    private String categoriaConcNomina;

    @Column(name = "es_salario")
    private Boolean esSalario;

    @Column(name = "es_ibc")
    private Boolean esIbc;

    @Column(name = "es_informativo")
    private Boolean esInformativo;

    @Column(name = "es_variable")
    private Boolean esVariable;

    @Column(name = "tipo_entrada_concept")
    private String tipoEntradaConcept;

    @Column(name = "responsable_pago_lic_inca")
    private String responsablePagoLicInca;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}