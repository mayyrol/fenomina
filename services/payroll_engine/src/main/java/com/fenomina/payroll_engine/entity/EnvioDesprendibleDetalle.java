package com.fenomina.payroll_engine.entity;

import com.fenomina.payroll_engine.enums.EstadoDetalleEnvio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "envio_desprendible_detalle", schema = "payroll")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvioDesprendibleDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "envio_desprendible_detalle_id")
    private Long envioDesprendibleDetalleId;

    @Column(name = "fk_envio_desprendible_id", nullable = false)
    private Long fkEnvioDesprendibleId;

    @Column(name = "correo_destino", nullable = false, length = 150)
    private String correoDestino;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_detalle", nullable = false, length = 20)
    private EstadoDetalleEnvio estadoDetalle;

    @Column(name = "mensaje_error", columnDefinition = "text")
    private String mensajeError;
}