package com.fenomina.payroll_engine.repository;

import com.fenomina.payroll_engine.entity.NominaCabecera;
import com.fenomina.payroll_engine.enums.EstadoProceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NominaCabeceraRepository extends JpaRepository<NominaCabecera, Long> {

    List<NominaCabecera> findByFkProcesoLiquiId(Long fkProcesoLiquiId);

    Optional<NominaCabecera> findByFkEmpleadoIdAndFkProcesoLiquiId(
            Long fkEmpleadoId,
            Long fkProcesoLiquiId
    );

    // Retorna únicamente el periodo pagado más reciente del empleado
    // Usado para obtener el IBC histórico en incapacidades y licencias no remuneradas
    @Query("""
            SELECT nc FROM NominaCabecera nc
            WHERE nc.fkEmpleadoId = :empleadoId
            AND nc.fkProcesoLiquiId IN (
                SELECT p.procesoLiquiId FROM ProcesoLiquidacion p
                WHERE p.estadoProcNomina = :estadoPagado
            )
            ORDER BY nc.anioCabecNomina DESC, nc.periodoCotiNomina DESC
            LIMIT 1
            """)
    Optional<NominaCabecera> findUltimoIbcByEmpleado(
            @Param("empleadoId") Long empleadoId,
            @Param("estadoPagado") EstadoProceso estadoPagado
    );

    @Query("""
            SELECT nc FROM NominaCabecera nc
            WHERE nc.fkEmpleadoId = :empleadoId
            AND nc.fkProcesoLiquiId IN (
                SELECT p.procesoLiquiId FROM ProcesoLiquidacion p
                WHERE p.estadoProcNomina = :estadoPagado
                AND p.anio = :anio
                AND p.periodo >= :periodoInicio
                AND p.periodo <= :periodoFin
            )
            ORDER BY nc.periodoCotiNomina ASC
            """)
    List<NominaCabecera> findByEmpleadoAndRangoPeriodo(
            @Param("empleadoId") Long empleadoId,
            @Param("anio") Integer anio,
            @Param("periodoInicio") Integer periodoInicio,
            @Param("periodoFin") Integer periodoFin,
            @Param("estadoPagado") EstadoProceso estadoPagado
    );

    @Query("SELECT COUNT(nc) FROM NominaCabecera nc WHERE nc.fkProcesoLiquiId = :procesoId")
    Integer countByFkProcesoLiquiId(@Param("procesoId") Long procesoId);

    @Query("SELECT COALESCE(SUM(nc.netoNominaEmp), 0) FROM NominaCabecera nc WHERE nc.fkProcesoLiquiId = :procesoId")
    BigDecimal sumNetoByFkProcesoLiquiId(@Param("procesoId") Long procesoId);

    @Query("""
        SELECT nc FROM NominaCabecera nc
        WHERE nc.fkEmpleadoId = :empleadoId
        AND nc.fkProcesoLiquiId IN (
            SELECT p.procesoLiquiId FROM ProcesoLiquidacion p
            WHERE p.fkIdEmpresa = :empresaId
            AND p.estadoProcNomina = 'PAGADO'
            AND p.fechaFinPeriodo >= :fechaInicio
            AND p.fechaFinPeriodo <= :fechaFin
            AND p.tipoProceso IN ('NOMINA_MENSUAL', 'NOMINA_QUINCENAL')
        )
        ORDER BY nc.periodoCotiNomina ASC
        """)
    List<NominaCabecera> findNominasDelSemestre(
            @Param("empleadoId") Long empleadoId,
            @Param("empresaId") Long empresaId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("SELECT nc.fkProcesoLiquiId, COUNT(nc) " +
            "FROM NominaCabecera nc " +
            "WHERE nc.fkProcesoLiquiId IN :procesoIds " +
            "GROUP BY nc.fkProcesoLiquiId")
    List<Object[]> countByProcesoIds(@Param("procesoIds") List<Long> procesoIds);

    @Query("SELECT nc.fkProcesoLiquiId, COALESCE(SUM(nc.netoNominaEmp), 0) " +
            "FROM NominaCabecera nc " +
            "WHERE nc.fkProcesoLiquiId IN :procesoIds " +
            "GROUP BY nc.fkProcesoLiquiId")
    List<Object[]> sumNetoByProcesoIds(@Param("procesoIds") List<Long> procesoIds);
}
