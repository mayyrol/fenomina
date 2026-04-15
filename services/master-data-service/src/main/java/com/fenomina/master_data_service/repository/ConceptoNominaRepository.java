package com.fenomina.master_data_service.repository;

import com.fenomina.master_data_service.entity.ConceptoNomina;
import com.fenomina.master_data_service.enums.CategoriaConcepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConceptoNominaRepository extends JpaRepository<ConceptoNomina, Long> {

    Optional<ConceptoNomina> findByNombreConcepNomina(String nombreConcepNomina);

    boolean existsByNombreConcepNomina(String nombreConcepNomina);

    @Query("SELECT c FROM ConceptoNomina c WHERE c.categoriaConcNomina = :categoria ORDER BY c.nombreConcepNomina")
    List<ConceptoNomina> findByCategoria(@Param("categoria") CategoriaConcepto categoria);

    @Query("SELECT c FROM ConceptoNomina c WHERE c.periodiConcepto.periodiConceptoId = :periodiId ORDER BY c.nombreConcepNomina")
    List<ConceptoNomina> findByPeriodicidad(@Param("periodiId") Long periodiId);

    @Query("SELECT c FROM ConceptoNomina c WHERE " +
            "(:categoria IS NULL OR c.categoriaConcNomina = :categoria) AND " +
            "(:periodiId IS NULL OR c.periodiConcepto.periodiConceptoId = :periodiId) " +
            "ORDER BY c.nombreConcepNomina")
    List<ConceptoNomina> findByFilters(@Param("categoria") CategoriaConcepto categoria,
                                       @Param("periodiId") Long periodiId);

    List<ConceptoNomina> findByEsSalarioTrue();

    List<ConceptoNomina> findByEsIbcTrue();

    @Query("SELECT c FROM ConceptoNomina c WHERE c.nombreConcepNomina IN (" +
            "'Beneficios o extralegales no salariales', " +
            "'Bonificaciones habituales', " +
            "'Viáticos permanentes manutención y alojamiento', " +
            "'Otros pagos que constituyen salario', " +
            "'Otros pagos que no constituyen salario permanente') " +
            "ORDER BY c.nombreConcepNomina")
    List<ConceptoNomina> findConceptosDisponiblesParaContrato();
}