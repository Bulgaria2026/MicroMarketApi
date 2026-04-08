package com.noserbulgaria.micromarket.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI documentation generation. Specifies server information and security requirements.
 *
 * @author Cyrill Weber
 */
@OpenAPIDefinition(
    servers = {@Server(description = "localhost", url = "http://localhost:8080/api/v1")},
    security = {@SecurityRequirement(name = "bearerAuth")}
)
@Configuration
public class OpenApiConfig {

  /**
   * Creates and configures a custom OpenAPI specification.
   *
   * @return A custom {@link io.swagger.v3.oas.models.OpenAPI} instance with specific information and security settings.
   */
  @Bean
  public OpenAPI customApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("MicroMarket API")
                .version("0.0.1-SNAPSHOT")
                .contact(new Contact().name("Noser Bulgaria")).
                description("The API to process all requests of the MicroMarket frontend. MicroMarket is a project of Noser Bulgaria")
        ).components(
            new Components().addSecuritySchemes(
                "bearerAuth",
                new SecurityScheme().
                    type(SecurityScheme.Type.HTTP).
                    scheme("bearer").
                    description("Auth with JWT Token").
                    bearerFormat("JWT")
            )
        );
  }
}
