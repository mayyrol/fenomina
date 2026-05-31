package com.fenomina.payroll_engine.repository;

import com.fenomina.payroll_engine.entity.CabeceraLiquiPrestacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CabeceraLiquiPrestacionRepository extends JpaRepository<CabeceraLiquiPrestacion, Long> {

    // Cabecera de prestación asociada a un proceso
    Optional<CabeceraLiquiPrestacion> findByFkProcesoLiquiId(Long fkProcesoLiquiId);
    @Query("SELECT c FROM CabeceraLiquiPrestacion c " +
            "WHERE c.fkProcesoLiquiId IN :procesoIds")
    List<CabeceraLiquiPrestacion> findByProcesoIds(
            @Param("procesoIds") List<Long> procesoIds);
}
