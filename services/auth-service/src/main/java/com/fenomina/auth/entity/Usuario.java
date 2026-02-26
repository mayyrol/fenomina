package com.fenomina.auth.entity;

import com.fenomina.auth.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario", schema = "auth")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "user_name", unique = true, nullable = false, length = 50)
    private String userName;

    @Column(name = "nombres_usuario", nullable = false)
    private String nombresUsuario;

    @Column(name = "apellidos_usuario", nullable = false)
    private String apellidosUsuario;

    @Column(name = "num_identi_usuario", unique = true, nullable = false, length = 50)
    private String numIdentiUsuario;

    @Column(name = "cargo_usuario", nullable = false, length = 60)
    private String cargoUsuario;

    @Column(name = "contrasena_usuario", nullable = false, length = 60)
    private String contrasenaUsuario;

    @Column(name = "fk_id_empresa")
    private Long fkIdEmpresa; // NULL = acceso a todas las empresas

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_usuario", nullable = false, length = 20)
    private RolUsuario rolUsuario;

    @Column(name = "estado_usuario", nullable = false)
    private Boolean estadoUsuario = true;

    @Column(name = "intentos_fallidos_login")
    private Integer intentosFallidosLogin = 0;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    // Auditoría automática
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

    // Métodos de negocio
    public void incrementarIntentosLogin() {
        this.intentosFallidosLogin++;
    }

    public void resetearIntentosLogin() {
        this.intentosFallidosLogin = 0;
        this.fechaBloqueo = null;
    }

    public void bloquear() {
        this.estadoUsuario = false;
        this.fechaBloqueo = LocalDateTime.now();
    }

    public boolean puedeDesbloquearseAutomaticamente() {
        if (fechaBloqueo == null || estadoUsuario) {
            return false;
        }
        return fechaBloqueo.plusMinutes(15).isBefore(LocalDateTime.now());
    }

    public void desbloquear() {
        this.estadoUsuario = true;
        this.intentosFallidosLogin = 0;
        this.fechaBloqueo = null;
    }

    public boolean estaActivo() {
        return estadoUsuario && deletedAt == null;
    }
}