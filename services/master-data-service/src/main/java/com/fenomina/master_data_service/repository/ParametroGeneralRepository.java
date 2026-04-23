package com.fenomina.master_data_service.repository;

import com.fenomina.master_data_service.entity.ParametroGeneral;
import com.fenomina.master_data_service.enums.ParametroNombre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParametroGeneralRepository extends JpaRepository<ParametroGeneral, Long> {

    List<ParametroGeneral> findAllByOrderByFechaParamGeneralDesc();

    @Query("SELECT p FROM ParametroGeneral p WHERE p.nombreParamGeneral = :nombre AND p.fechaParamGeneral <= :fecha ORDER BY p.fechaParamGeneral DESC")
    List<ParametroGeneral> findVigentesByNombreAndFecha(@Param("nombre") ParametroNombre nombre, @Param("fecha") LocalDate fecha);

    @Query("SELECT p FROM ParametroGeneral p WHERE p.nombreParamGeneral = :nombre AND p.fechaParamGeneral <= :fecha ORDER BY p.fechaParamGeneral DESC LIMIT 1")
    Optional<ParametroGeneral> findVigenteByNombreAndFecha(@Param("nombre") ParametroNombre nombre, @Param("fecha") LocalDate fecha);

    List<ParametroGeneral> findByNombreParamGeneral(ParametroNombre nombreParamGeneral);

    boolean existsByNombreParamGeneralAndFechaParamGeneral(
            ParametroNombre nombreParamGeneral,
            LocalDate fechaParamGeneral
    );
}
