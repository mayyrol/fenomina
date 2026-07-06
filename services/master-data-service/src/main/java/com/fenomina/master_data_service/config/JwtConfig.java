package com.fenomina.master_data_service.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:7200000}")
    private Long expiration;

    public String getTokenHeader() {
        return "Authorization";
    }

    public String getTokenPrefix() {
        return "Bearer ";
    }

    public String getIssuer() {
        return "fenomina-auth-service";
    }
}
