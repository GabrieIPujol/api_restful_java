package com.senac.tsi.Formula1Api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

// CORS: quais sites podem chamar a api direto do navegador
// so as origens da propriedade app.cors.allowed-origins, qualquer outro site e barrado pelo navegador
// (Postman, Thunder Client e curl nao ligam pra CORS, isso e coisa de navegador)
@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        // metodos que a api usa
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // headers que o navegador pode mandar
        config.setAllowedHeaders(List.of("Content-Type", "Accept", ApiKeyFilter.HEADER, IdempotencyFilter.HEADER, ApiVersionConfig.HEADER));
        // headers da resposta que o javascript do site pode ler
        config.setExposedHeaders(List.of("Location", "Idempotent-Replayed", "Deprecation", "Sunset",
                "Retry-After", "X-Rate-Limit-Limit", "X-Rate-Limit-Remaining"));
        // o navegador guarda a resposta do preflight por 1 hora
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // ordem 0: roda antes dos outros filtros, assim ate o 401 e o 429 voltam com os headers de CORS
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(0);
        return registration;
    }
}
