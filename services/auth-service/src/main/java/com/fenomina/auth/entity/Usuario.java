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

    @Column(name = "bloqueado_login", nullable = false)
    @Builder.Default
    private Boolean bloqueadoLogin = false; // Bloqueado por login fallido

    @Column(name = "intentos_fallidos_login")
    @Builder.Default
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

    //eliminar luego ****
    public void bloquear() {
        this.estadoUsuario = false;
        this.fechaBloqueo = LocalDateTime.now();
    }

    public void incrementarIntentosLogin() {
        this.intentosFallidosLogin++;
        if (this.intentosFallidosLogin >= 5) {
            bloquearPorIntentosLogin();
        }
    }

    public void resetearIntentosLogin() {
        this.intentosFallidosLogin = 0;
        this.bloqueadoLogin = false;
        this.fechaBloqueo = null;
    }

    private void bloquearPorIntentosLogin() {
        this.bloqueadoLogin = true;
        this.fechaBloqueo = LocalDateTime.now();
    }

    public boolean puedeDesbloquearseAutomaticamente() {
        if (fechaBloqueo == null || !bloqueadoLogin) {
            return false;
        }
        return fechaBloqueo.plusMinutes(15).isBefore(LocalDateTime.now());
    }

    public void desbloquearLogin() {
        this.bloqueadoLogin = false;
        this.intentosFallidosLogin = 0;
        this.fechaBloqueo = null;
    }

    public void activar() {
        this.estadoUsuario = true;
    }

    public void inactivar() {
        this.estadoUsuario = false;
    }

    public boolean puedeHacerLogin() {
        return estadoUsuario && !bloqueadoLogin && deletedAt == null;
    }

    public boolean estaActivo() {
        return deletedAt == null;
    }

}