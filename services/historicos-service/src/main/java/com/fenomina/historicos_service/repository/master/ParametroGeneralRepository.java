package com.fenomina.historicos_service.repository.master;

import com.fenomina.historicos_service.entity.master.ParametroGeneral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ParametroGeneralRepository extends JpaRepository<ParametroGeneral, Long> {

    @Query("""
        SELECT p FROM ParametroGeneral p
        WHERE LOWER(p.nombreParamGeneral) LIKE LOWER(CONCAT('%', :nombre, '%'))
          AND p.fechaParamGeneral <= :fecha
        ORDER BY p.fechaParamGeneral DESC
        """)
    Optional<ParametroGeneral> findVigenteByNombreYFecha(
            @Param("nombre") String nombre,
            @Param("fecha") LocalDate fecha
    );
}
