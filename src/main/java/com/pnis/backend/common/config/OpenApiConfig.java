package com.pnis.backend.common.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.*;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pnisOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PNIS Backend API")
                        .description("Plateforme Nationale Mutualisée de Renseignement – Gabon. Backend unifié v2.0")
                        .version("2.0.0")
                        .contact(new Contact().name("PNIS Gabon").email("api@pnis.gov.ga"))
                        .license(new License().name("Propriétaire – Usage gouvernemental exclusif")))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api/v1").description("Développement local"),
                        new Server().url("https://api.pnis.gov.ga/api/v1").description("Production")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .name("BearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Token JWT obtenu via POST /auth/login")));
    }
}
