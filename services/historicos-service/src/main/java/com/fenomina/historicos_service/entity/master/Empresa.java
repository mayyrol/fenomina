package com.fenomina.historicos_service.entity.master;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "empresa", schema = "master_data")
@Where(clause = "deleted_at IS NULL")
public class Empresa {

    @Id
    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "empresa_nit")
    private String empresaNit;

    @Column(name = "razon_social")
    private String razonSocial;

    @Column(name = "nombre_empresa")
    private String nombreEmpresa;

    @Column(name = "es_exonerada_ley1607")
    private Boolean esExoneradaLey1607;

    @Column(name = "aplica_nomina")
    private Boolean aplicaNomina;

    @Column(name = "aplica_prima")
    private Boolean aplicaPrima;

    @Column(name = "aplica_cesantias")
    private Boolean aplicaCesantias;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
