package com.fenomina.historicos_service.repository.master;

import com.fenomina.historicos_service.entity.master.ConceptoNomina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptoNominaRepository extends JpaRepository<ConceptoNomina, Long> {
    List<ConceptoNomina> findByCategoriaConcNomina(String categoria);
}
