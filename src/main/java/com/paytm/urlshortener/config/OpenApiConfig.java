package com.paytm.urlshortener.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration for SpringDoc.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("Paytm URL Shortener API")
                        .version("1.0.0")
                        .description("API documentation for the Paytm-style URL Shortener service")
                        .contact(new Contact().name("Paytm Engineering").url("https://paytm.com"))
                        .license(new License().name("Proprietary")))
                .servers(List.of(new Server().url("/")));
    }
}
