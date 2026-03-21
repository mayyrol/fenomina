package com.fenomina.auth.repository;

import com.fenomina.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false")
    Optional<RefreshToken> findValidToken(@Param("token") String token);

    @Transactional
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.usuario.usuarioId = :usuarioId AND rt.revoked = false")
    int revokeAllUserTokens(@Param("usuarioId") Long usuarioId);

    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.usuario.usuarioId = :usuarioId AND rt.revoked = false")
    long countActiveTokensByUsuario(@Param("usuarioId") Long usuarioId);

    //Query optimizado para obtener el token mas antiguo
    @Query("""
        SELECT rt FROM RefreshToken rt 
        WHERE rt.usuario.usuarioId = :usuarioId 
          AND rt.revoked = false
        ORDER BY rt.createdAt ASC
        LIMIT 1
        """)
    Optional<RefreshToken> findOldestActiveToken(@Param("usuarioId") Long usuarioId);
}