package com.fenomina.payroll_engine.client;

import feign.RequestInterceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class FeignInternalApiKeyConfig {

    @Bean
    public RequestInterceptor internalApiKeyInterceptor(
            @Value("${internal.api.key}") String internalApiKey
    ) {
        return template -> template.header("X-Internal-Api-Key", internalApiKey);
    }
}
