package com.fenomina.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuración para habilitar ejecución asíncrona de métodos.
 * Permite que los registros de auditoría no bloqueen operaciones críticas.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

}
