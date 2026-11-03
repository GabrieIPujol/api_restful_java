package com.senac.tsi.Formula1Api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

// versionamento da api usando o suporte nativo do Spring 7, igual o professor fez
// o cliente escolhe a versao de um desses jeitos (vale o primeiro que achar):
//   - header:          API-Version: 2
//   - parametro na url: /drivers?api-version=2
// sem versao a requisicao cai na v1, entao quem ja usava a api continua funcionando
// os controllers dizem a versao de cada endpoint com @GetMapping(path = "...", version = "2")
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    public static final String HEADER = "API-Version";
    public static final String QUERY_PARAM = "api-version";

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        // a v1 dos pilotos continua funcionando, mas toda resposta dela avisa com os headers
        // Deprecation e Sunset que ela vai ser desligada (RFC 9745 / RFC 8594)
        StandardApiVersionDeprecationHandler deprecation = new StandardApiVersionDeprecationHandler();
        deprecation.configureVersion("1")
                .setDeprecationDate(ZonedDateTime.of(2026, 11, 3, 0, 0, 0, 0, ZoneOffset.UTC))
                .setSunsetDate(ZonedDateTime.of(2027, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                // o resto da api (equipes, swagger...) tambem cai na versao padrao, so avisa nos pilotos
                .setRequestPredicate(request -> request.getRequestURI().startsWith("/drivers"));

        configurer
                .useRequestHeader(HEADER)
                .useQueryParam(QUERY_PARAM)
                .setDefaultVersion("1")
                .setDeprecationHandler(deprecation);
        // as versoes aceitas (1 e 2) saem do atributo "version" dos mapeamentos,
        // pedir qualquer outra (ex.: 3) volta 400
    }
}
