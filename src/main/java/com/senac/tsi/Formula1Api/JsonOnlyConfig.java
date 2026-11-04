package com.senac.tsi.Formula1Api;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// a api so fala JSON: a biblioteca de YAML vem junto com o swagger (springdoc) e o spring 7 passava
// a aceitar e devolver YAML sozinho. tirando esse conversor, corpo em YAML da 415 e pedir YAML da 406
@Configuration
public class JsonOnlyConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        // filtra pelo tipo e nao pela classe, porque o conversor muda conforme a versao do Jackson que vier
        builder.configureMessageConvertersList(converters ->
                converters.removeIf(converter -> converter.getSupportedMediaTypes().contains(MediaType.APPLICATION_YAML)));
    }
}
