package com.fenomina.historicos_service.entity.auth;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "usuario", schema = "auth")
@Where(clause = "deleted_at IS NULL")
public class Usuario {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "nombres_usuario")
    private String nombresUsuario;

    @Column(name = "apellidos_usuario")
    private String apellidosUsuario;

    @Column(name = "num_identi_usuario")
    private String numIdentiUsuario;

    @Column(name = "cargo_usuario")
    private String cargoUsuario;

    @Column(name = "fk_id_empresa")
    private Long fkIdEmpresa;

    @Column(name = "rol_usuario")
    private String rolUsuario;

    @Column(name = "estado_usuario")
    private Boolean estadoUsuario;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}