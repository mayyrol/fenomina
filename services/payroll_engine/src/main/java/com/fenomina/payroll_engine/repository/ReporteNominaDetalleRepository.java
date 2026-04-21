package com.fenomina.payroll_engine.repository;

import com.fenomina.payroll_engine.entity.ReporteNominaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteNominaDetalleRepository extends JpaRepository<ReporteNominaDetalle, Long> {

    // Todos los conceptos calculados para una cabecera, usado en desprendibles
    List<ReporteNominaDetalle> findByFkCabecNominaId(Long fkCabecNominaId);

    // Conceptos de un tipo específico en una cabecera, usado por el motor para verificar
    // si ya fue calculado un concepto antes de persistir
    boolean existsByFkCabecNominaIdAndFkConcepNominaId(Long fkCabecNominaId, Long fkConcepNominaId);
}