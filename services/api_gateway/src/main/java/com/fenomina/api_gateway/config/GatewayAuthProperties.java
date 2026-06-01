package com.fenomina.api_gateway.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "gateway.auth")
@Getter
@Setter
public class GatewayAuthProperties {
    private List<String> excludedPaths;
}