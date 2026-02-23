package com.fenomina.auth.security;

import com.fenomina.auth.entity.Usuario;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Implementación de UserDetails de Spring Security.
 */
@RequiredArgsConstructor
@Getter
public class CustomUserDetails implements UserDetails {

    private final Usuario usuario;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security espera roles con prefijo "ROLE_"
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRolUsuario().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getContrasenaUsuario();
    }

    @Override
    public String getUsername() {
        return usuario.getUserName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Las cuentas no expiran en este sistema
    }

    @Override
    public boolean isAccountNonLocked() {
        return usuario.getEstadoUsuario(); // true = no bloqueado, false = bloqueado
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Las credenciales no expiran
    }

    @Override
    public boolean isEnabled() {
        return usuario.getDeletedAt() == null; // Usuario activo si no está eliminado
    }

    public Long getUserId() {
        return usuario.getUsuarioId();
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
