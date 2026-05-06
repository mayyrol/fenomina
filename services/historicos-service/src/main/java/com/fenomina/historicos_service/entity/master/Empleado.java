package com.fenomina.historicos_service.entity.master;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "empleado", schema = "master_data")
@Where(clause = "deleted_at IS NULL")
public class Empleado {

    @Id
    @Column(name = "empleado_id")
    private Long empleadoId;

    @Column(name = "fk_id_empresa")
    private Long fkIdEmpresa;

    @Column(name = "tipo_documento")
    private String tipoDocumento;

    @Column(name = "documento_emp")
    private String documentoEmp;

    @Column(name = "nombres_emp")
    private String nombresEmp;

    @Column(name = "apellidos_emp")
    private String apellidosEmp;

    @Column(name = "fecha_ingreso_emp")
    private LocalDate fechaIngresoEmp;

    @Column(name = "fecha_retiro_emp")
    private LocalDate fechaRetiroEmp;

    @Column(name = "cargo_emp")
    private String cargoEmp;

    @Column(name = "es_salario_integral")
    private Boolean esSalarioIntegral;

    @Column(name = "salario_basc_mensual")
    private BigDecimal salarioBascMensual;

    @Column(name = "tiene_aux_transporte")
    private Boolean tieneAuxTransporte;

    @Column(name = "clase_riesgo")
    private String claseRiesgo;

    @Column(name = "nombre_arl")
    private String nombreArl;

    @Column(name = "nombre_eps")
    private String nombreEps;

    @Column(name = "fondo_pension_emp")
    private String fondoPensionEmp;

    @Column(name = "caja_compensacion")
    private String cajaCompensacion;

    @Column(name = "fondo_cesantias_emp")
    private String fondoCesantiasEmp;

    @Column(name = "esta_exnrd_parafis")
    private Boolean estaExnrdParafis;

    @Column(name = "estado_emp")
    private String estadoEmp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
