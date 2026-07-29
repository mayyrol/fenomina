package com.fenomina.payroll_engine.repository;

import com.fenomina.payroll_engine.entity.EnvioDesprendible;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnvioDesprendibleRepository extends JpaRepository<EnvioDesprendible, Long> {

    List<EnvioDesprendible> findByFkProcesoLiquiIdOrderByCreatedAtDesc(Long fkProcesoLiquiId);
}