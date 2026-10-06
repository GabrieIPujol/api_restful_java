package com.senac.tsi.Formula1Api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApi {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Formula 1 API")
                        .version("1.0.0")
                        .description("API RESTful para gerenciar equipes, chefes de equipe, pilotos, patrocinadores " +
                                "e resultados de corrida da Formula 1, com tabelas de classificacao de pilotos e construtores. " +
                                "Todas as listagens sao paginadas (page, size, sort) e as respostas seguem HATEOAS (HAL).")
                        .termsOfService("https://swagger.io/terms/")
                        .license(new License().name("MIT").url("https://mit-license.org/"))
                        .contact(new Contact().name("TSI")
                                .url("https://www.senac.com.br")
                                .email("****@sp.senac.br"))
                );
    }
}
