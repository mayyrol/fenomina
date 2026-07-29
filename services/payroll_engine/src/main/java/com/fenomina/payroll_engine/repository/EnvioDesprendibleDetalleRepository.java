package com.fenomina.payroll_engine.repository;

import com.fenomina.payroll_engine.entity.EnvioDesprendibleDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnvioDesprendibleDetalleRepository extends JpaRepository<EnvioDesprendibleDetalle, Long> {

    List<EnvioDesprendibleDetalle> findByFkEnvioDesprendibleId(Long fkEnvioDesprendibleId);
}