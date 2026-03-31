package com.fenomina.master_data_service.repository;

import com.fenomina.master_data_service.entity.HistorialSalario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialSalarioRepository extends JpaRepository<HistorialSalario, Long> {

    @Query("SELECT h FROM HistorialSalario h WHERE h.empleado.empleadoId = :empleadoId ORDER BY h.createdAt DESC")
    List<HistorialSalario> findByEmpleadoIdOrderByCreatedAtDesc(@Param("empleadoId") Long empleadoId);

    @Query("SELECT h FROM HistorialSalario h WHERE h.empleado.empleadoId = :empleadoId ORDER BY h.createdAt DESC LIMIT 1")
    HistorialSalario findLastByEmpleadoId(@Param("empleadoId") Long empleadoId);
}
