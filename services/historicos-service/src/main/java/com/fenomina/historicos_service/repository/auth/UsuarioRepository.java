package com.fenomina.historicos_service.repository.auth;

import com.fenomina.historicos_service.entity.auth.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUserName(String userName);
}
