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
@Table(name = "empresa_correo", schema = "master_data")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaCorreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "empresa_correo_id")
    private Long empresaCorreoId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

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