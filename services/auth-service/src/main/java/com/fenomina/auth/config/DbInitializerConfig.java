package com.fenomina.auth.config;

import com.fenomina.auth.entity.Usuario;
import com.fenomina.auth.enums.RolUsuario;
import com.fenomina.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Inicializador de base de datos.
 * Crea el usuario maestro (SUPER_ADMIN) si la BD está vacía.
 * Solo se ejecuta una vez al arrancar la aplicación por primera vez.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DbInitializerConfig {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            // Verificar si ya existen usuarios en la BD
            if (usuarioRepository.count() == 0) {
                log.info("========================================");
                log.info("BASE DE DATOS VACÍA - Creando usuario maestro...");
                log.info("========================================");

                // Crear usuario maestro (SUPER_ADMIN)
                Usuario adminMaestro = Usuario.builder()
                        .userName("adminfe")
                        .nombresUsuario("Administrador")
                        .apellidosUsuario("Sistema FEN")
                        .numIdentiUsuario("000000000")
                        .cargoUsuario("Super Administrador")
                        .contrasenaUsuario(passwordEncoder.encode("AdminFE@2026"))
                        .rolUsuario(RolUsuario.SUPER_ADMIN)
                        .fkIdEmpresa(null) // Acceso a todas las empresas
                        .estadoUsuario(true)
                        .intentosFallidosLogin(0)
                        .build();

                usuarioRepository.save(adminMaestro);

                log.info("Usuario maestro creado exitosamente :)");
                log.info("========================================");
                log.info("CREDENCIALES DE ACCESO INICIAL");
                log.info("Username: adminfe");
                log.info("Password: AdminFE@2026");
                log.info("========================================");
                log.info(" IMPORTANTE: Cambiar estas credenciales en producción");
                log.info("========================================");
            } else {
                log.info("Base de datos ya inicializada. Total usuarios: {}", usuarioRepository.count());
            }
        };
    }
}