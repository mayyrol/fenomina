package com.fenomina.master_data_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "empresa", schema = "master_data")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "empresa_nit", nullable = false, unique = true, length = 30)
    private String empresaNit;

    @Column(name = "razon_social", nullable = false, length = 70)
    private String razonSocial;

    @Column(name = "nombre_empresa", nullable = false, length = 70)
    private String nombreEmpresa;

    @Column(name = "es_exonerada_ley1607", nullable = false)
    private Boolean esExoneradaLey1607 = false;

    @Column(name = "logo_empresa_url")
    private String logoEmpresaUrl;

    @Column(name = "aplica_nomina", nullable = false)
    private Boolean aplicaNomina = true;

    @Column(name = "aplica_prima", nullable = false)
    private Boolean aplicaPrima = true;

    @Column(name = "aplica_cesantias", nullable = false)
    private Boolean aplicaCesantias = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}