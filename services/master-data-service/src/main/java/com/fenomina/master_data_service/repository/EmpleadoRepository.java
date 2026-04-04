package com.fenomina.master_data_service.repository;

import com.fenomina.master_data_service.entity.Empleado;
import com.fenomina.master_data_service.enums.EstadoEmpleado;
import com.fenomina.master_data_service.enums.TipoDocumento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    @EntityGraph(attributePaths = {"empresa"})
    @Query("SELECT e FROM Empleado e WHERE e.deletedAt IS NULL")
    List<Empleado> findAllActiveWithEmpresa();

    @EntityGraph(attributePaths = {"empresa"})
    @Query("SELECT e FROM Empleado e WHERE e.empleadoId = :id AND e.deletedAt IS NULL")
    Optional<Empleado> findByIdActiveWithEmpresa(@Param("id") Long id);

    @EntityGraph(attributePaths = {"empresa"})
    @Query("SELECT e FROM Empleado e WHERE e.deletedAt IS NULL AND " +
            "(:empresaId IS NULL OR e.empresa.empresaId = :empresaId) AND " +
            "(:estado IS NULL OR e.estadoEmp = :estado) AND " +
            "(:documento IS NULL OR e.documentoEmp = :documento)")
    List<Empleado> findByFilters(@Param("empresaId") Long empresaId,
                                 @Param("estado") EstadoEmpleado estado,
                                 @Param("documento") String documento);

    @Query("SELECT e FROM Empleado e WHERE e.empresa.empresaId = :empresaId AND e.deletedAt IS NULL")
    List<Empleado> findByEmpresaId(@Param("empresaId") Long empresaId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Empleado e WHERE e.empresa.empresaId = :empresaId AND e.documentoEmp = :documento AND e.deletedAt IS NULL")
    boolean existsByEmpresaIdAndDocumento(@Param("empresaId") Long empresaId, @Param("documento") String documento);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Empleado e WHERE e.empresa.empresaId = :empresaId AND e.documentoEmp = :documento AND e.empleadoId <> :id AND e.deletedAt IS NULL")
    boolean existsByEmpresaIdAndDocumentoAndNotId(@Param("empresaId") Long empresaId, @Param("documento") String documento, @Param("id") Long id);

    @Query("SELECT e FROM Empleado e WHERE e.estadoEmp = :estado AND e.deletedAt IS NULL")
    List<Empleado> findByEstado(@Param("estado") EstadoEmpleado estado);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Empleado e WHERE e.empresa.empresaId = :empresaId AND e.tipoDocumento = :tipoDocumento AND e.documentoEmp = :documento AND e.deletedAt IS NULL")
    boolean existsByEmpresaIdAndTipoDocumentoAndDocumento(@Param("empresaId") Long empresaId, @Param("tipoDocumento") TipoDocumento tipoDocumento, @Param("documento") String documento);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Empleado e WHERE e.empresa.empresaId = :empresaId AND e.tipoDocumento = :tipoDocumento AND e.documentoEmp = :documento AND e.empleadoId <> :id AND e.deletedAt IS NULL")
    boolean existsByEmpresaIdAndTipoDocumentoAndDocumentoAndNotId(@Param("empresaId") Long empresaId, @Param("tipoDocumento") TipoDocumento tipoDocumento, @Param("documento") String documento, @Param("id") Long id);
}