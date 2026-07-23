package com.fenomina.historicos_service.dto.notificaciones;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class NotificacionVacacionesDTO {
    private Long      id;
    private Long      fkEmpleadoId;
    private Long      fkIdEmpresa;
    private String    nombreEmpresa;
    private String    documentoEmp;
    private String    nombresEmp;
    private String    apellidosEmp;
    private LocalDate proximaFechaVac;
    private Integer   diasRestantes;
    private LocalDate fechaDisparo;
    private Boolean   leida;
    private LocalDateTime createdAt;
}