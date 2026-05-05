package com.example.accountservices.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI accountServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Account Service API")
                        .description("Manages bank account lifecycle: open, query, update balance, close")
                        .version("v1.0.0")
                        .contact(new Contact().name("Banking Platform Team").email("platform@banking.example.com"))
                        .license(new License().name("Internal Use Only")))
                .servers(List.of(
                        new Server().url("http://localhost:8001").description("Local Dev"),
                        new Server().url("http://localhost:8080/api/v1").description("Via API Gateway")
                ));
    }
}

