package com.fenomina.payroll_engine.repository;

import com.fenomina.payroll_engine.entity.CabeceraLiquiPrestacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CabeceraLiquiPrestacionRepository extends JpaRepository<CabeceraLiquiPrestacion, Long> {

    // Cabecera de prestación asociada a un proceso
    Optional<CabeceraLiquiPrestacion> findByFkProcesoLiquiId(Long fkProcesoLiquiId);
}
