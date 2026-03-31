package com.fenomina.master_data_service.entity;

import com.fenomina.master_data_service.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "empleado", schema = "master_data")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "empleado_id")
    private Long empleadoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_id_empresa", nullable = false)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 70)
    private TipoDocumento tipoDocumento;

    @Column(name = "documento_emp", nullable = false, length = 30)
    private String documentoEmp;

    @Column(name = "nombres_emp", nullable = false)
    private String nombresEmp;

    @Column(name = "apellidos_emp", nullable = false)
    private String apellidosEmp;

    @Column(name = "direccion_emp")
    private String direccionEmp;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato_emp", length = 50)
    private TipoContrato tipoContratoEmp;

    @Column(name = "fecha_ingreso_emp", nullable = false)
    private LocalDate fechaIngresoEmp;

    @Column(name = "fecha_retiro_emp")
    private LocalDate fechaRetiroEmp;

    @Column(name = "fecha_fin_contrato")
    private LocalDate fechaFinContrato;

    @Column(name = "cargo_emp", length = 60)
    private String cargoEmp;

    @Column(name = "es_salario_integral")
    private Boolean esSalarioIntegral = false;

    @Column(name = "salario_basc_mensual", nullable = false, precision = 15, scale = 2)
    private BigDecimal salarioBascMensual;

    @Column(name = "tiene_aux_transporte", nullable = false)
    private Boolean tieneAuxTransporte = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "clase_riesgo", nullable = false, length = 5)
    private ClaseRiesgo claseRiesgo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cotizante", nullable = false, length = 60)
    private TipoCotizante tipoCotizante;

    @Enumerated(EnumType.STRING)
    @Column(name = "subtipo_cotizante", nullable = false, length = 60)
    private SubtipoCotizante subtipoCotizante;

    @Column(name = "nombre_arl", nullable = false, length = 30)
    private String nombreArl;

    @Column(name = "nombre_eps", nullable = false, length = 30)
    private String nombreEps;

    @Column(name = "fondo_pension_emp", nullable = false, length = 60)
    private String fondoPensionEmp;

    @Column(name = "caja_compensacion", nullable = false, length = 60)
    private String cajaCompensacion;

    @Column(name = "fondo_cesantias_emp", nullable = false, length = 60)
    private String fondoCesantiasEmp;

    @Column(name = "esta_exnrd_parafis")
    private Boolean estaExnrdParafis = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "jornada_trabajo_emp", length = 60)
    private JornadaTrabajo jornadaTrabajoEmp;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_emp", length = 20)
    private EstadoEmpleado estadoEmp = EstadoEmpleado.ACTIVO;

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
