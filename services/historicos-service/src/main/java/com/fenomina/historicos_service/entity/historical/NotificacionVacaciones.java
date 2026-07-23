package com.fenomina.historicos_service.entity.historical;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones_vacaciones", schema = "historical")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionVacaciones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fk_empleado_id", nullable = false)
    private Long fkEmpleadoId;

    @Column(name = "fk_id_empresa", nullable = false)
    private Long fkIdEmpresa;

    @Column(name = "nombre_empresa")
    private String nombreEmpresa;

    @Column(name = "documento_emp")
    private String documentoEmp;

    @Column(name = "nombres_emp")
    private String nombresEmp;

    @Column(name = "apellidos_emp")
    private String apellidosEmp;

    @Column(name = "proxima_fecha_vac", nullable = false)
    private LocalDate proximaFechaVac;

    @Column(name = "dias_restantes", nullable = false)
    private Integer diasRestantes;

    @Column(name = "fecha_disparo", nullable = false)
    private LocalDate fechaDisparo;

    @Column(name = "leida")
    @Builder.Default
    private Boolean leida = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}