package com.fenomina.master_data_service.repository;

import com.fenomina.master_data_service.entity.ContratoConcepto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContratoConceptoRepository extends JpaRepository<ContratoConcepto, Long> {

    @EntityGraph(attributePaths = {"empleado", "conceptoNomina"})
    @Query("SELECT cc FROM ContratoConcepto cc WHERE cc.empleado.empleadoId = :empleadoId AND cc.deletedAt IS NULL")
    List<ContratoConcepto> findByEmpleadoIdActiveWithRelations(@Param("empleadoId") Long empleadoId);

    @Query("SELECT cc FROM ContratoConcepto cc WHERE cc.contratoConceptId = :id AND cc.deletedAt IS NULL")
    Optional<ContratoConcepto> findByIdActive(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(cc) > 0 THEN true ELSE false END FROM ContratoConcepto cc WHERE cc.empleado.empleadoId = :empleadoId AND cc.conceptoNomina.concepNominaId = :conceptoId AND cc.deletedAt IS NULL")
    boolean existsByEmpleadoAndConcepto(@Param("empleadoId") Long empleadoId, @Param("conceptoId") Long conceptoId);

    @Query("SELECT CASE WHEN COUNT(cc) > 0 THEN true ELSE false END FROM ContratoConcepto cc WHERE cc.empleado.empleadoId = :empleadoId AND cc.conceptoNomina.concepNominaId = :conceptoId AND cc.contratoConceptId <> :id AND cc.deletedAt IS NULL")
    boolean existsByEmpleadoAndConceptoAndNotId(@Param("empleadoId") Long empleadoId, @Param("conceptoId") Long conceptoId, @Param("id") Long id);


}