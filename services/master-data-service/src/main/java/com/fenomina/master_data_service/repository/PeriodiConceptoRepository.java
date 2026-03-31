package com.fenomina.master_data_service.repository;

import com.fenomina.master_data_service.entity.PeriodiConcepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PeriodiConceptoRepository extends JpaRepository<PeriodiConcepto, Long> {

    Optional<PeriodiConcepto> findByNombrePeriodi(String nombrePeriodi);

    boolean existsByNombrePeriodi(String nombrePeriodi);
}
