package com.fenomina.historicos_service.repository.master;

import com.fenomina.historicos_service.entity.master.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    List<Empresa> findAllByOrderByNombreEmpresaAsc();
}