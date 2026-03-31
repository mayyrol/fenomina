package com.fenomina.master_data_service.repository;

import com.fenomina.master_data_service.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    @Query("SELECT e FROM Empresa e WHERE e.deletedAt IS NULL")
    List<Empresa> findAllActive();

    @Query("SELECT e FROM Empresa e WHERE LOWER(e.nombreEmpresa) LIKE LOWER(CONCAT('%', :nombre, '%')) AND e.deletedAt IS NULL")
    List<Empresa> findByNombreContaining(@Param("nombre") String nombre);

    @Query("SELECT e FROM Empresa e WHERE e.empresaId = :id AND e.deletedAt IS NULL")
    Optional<Empresa> findByIdActive(@Param("id") Long id);

    @Query("SELECT e FROM Empresa e WHERE e.empresaNit = :nit AND e.deletedAt IS NULL")
    Optional<Empresa> findByNitActive(@Param("nit") String nit);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Empresa e WHERE e.empresaNit = :nit AND e.deletedAt IS NULL")
    boolean existsByNitActive(@Param("nit") String nit);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Empresa e WHERE e.empresaNit = :nit AND e.empresaId <> :id AND e.deletedAt IS NULL")
    boolean existsByNitAndNotId(@Param("nit") String nit, @Param("id") Long id);
}