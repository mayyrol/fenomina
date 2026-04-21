package com.fenomina.payroll_engine.service.novedad;

import com.fenomina.payroll_engine.entity.Novedad;

import java.util.List;

public interface NovedadService {

    Novedad crear(Novedad novedad);

    Novedad actualizar(Long novedadId, Novedad novedad);

    void eliminar(Long novedadId);

    Novedad findById(Long novedadId);

    List<Novedad> findByProceso(Long procesoId);

    List<Novedad> findByEmpleadoYProceso(Long empleadoId, Long procesoId);
}
