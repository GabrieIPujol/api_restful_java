package com.senac.tsi.Formula1Api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// testa os recursos da parte 2 com a aplicacao inteira de pe (filtros, controllers e banco)
// pra rodar: .\mvnw.cmd test
@SpringBootTest
@AutoConfigureMockMvc
class Part2FeaturesTests {

    // as chaves de teste do src/test/resources/config/application.properties
    private static final String USER_KEY = "test-user-key";
    private static final String ADMIN_KEY = "test-admin-key";

    @Autowired
    private MockMvc mvc;

    // cada teste usa um IP diferente pra um nao gastar o rate limit do outro
    private static RequestPostProcessor ip(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    @Test
    void writeNeedsApiKeyAndManagingKeysNeedsAdmin() throws Exception {
        String body = "{\"name\":\"Sponsor Teste Chave\",\"industry\":\"Energy\"}";

        // leitura e livre, mas chave errada no GET da 401 em vez de ser ignorada
        mvc.perform(get("/sponsors").with(ip("10.1.0.1"))).andExpect(status().isOk());
        mvc.perform(get("/sponsors").with(ip("10.1.0.1")).header("X-API-Key", "chave-que-nao-existe"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/sponsors").with(ip("10.1.0.1")).header("X-API-Key", USER_KEY)).andExpect(status().isOk());

        // escrita sem chave volta 401
        mvc.perform(post("/sponsors").with(ip("10.1.0.1")).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/sponsors").with(ip("10.1.0.1")).header("X-API-Key", "chave-que-nao-existe")
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        // USER escreve mas nao gerencia chaves, ADMIN gerencia
        mvc.perform(post("/sponsors").with(ip("10.1.0.1")).header("X-API-Key", USER_KEY)
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(get("/api-keys").with(ip("10.1.0.1")).header("X-API-Key", USER_KEY))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api-keys").with(ip("10.1.0.1")).header("X-API-Key", ADMIN_KEY))
                .andExpect(status().isOk());
    }

    @Test
    void generatedKeyWorksUntilItIsRevoked() throws Exception {
        String created = mvc.perform(post("/api-keys").with(ip("10.1.0.2")).header("X-API-Key", ADMIN_KEY)
                        .contentType(APPLICATION_JSON).content("{\"name\":\"Chave de teste\",\"role\":\"USER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").exists())
                .andReturn().getResponse().getContentAsString();
        String key = JsonPath.read(created, "$.key");
        Integer id = JsonPath.read(created, "$.id");

        // a chave aberta so aparece na criacao
        mvc.perform(get("/api-keys/" + id).with(ip("10.1.0.2")).header("X-API-Key", ADMIN_KEY))
                .andExpect(jsonPath("$.key").doesNotExist());

        String body = "{\"name\":\"Sponsor Chave Gerada\",\"industry\":\"Energy\"}";
        mvc.perform(post("/sponsors").with(ip("10.1.0.2")).header("X-API-Key", key)
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api-keys/" + id).with(ip("10.1.0.2")).header("X-API-Key", ADMIN_KEY))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/sponsors/1").with(ip("10.1.0.2")).header("X-API-Key", key))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedIdempotencyKeyReturnsTheFirstResponse() throws Exception {
        String body = "{\"name\":\"Sponsor Idempotente\",\"industry\":\"Idempotency\"}";

        String first = mvc.perform(post("/sponsors").with(ip("10.1.0.3")).header("X-API-Key", USER_KEY)
                        .header("Idempotency-Key", "teste-idempotencia-1").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // a segunda nao cria nada, so devolve a primeira resposta
        mvc.perform(post("/sponsors").with(ip("10.1.0.3")).header("X-API-Key", USER_KEY)
                        .header("Idempotency-Key", "teste-idempotencia-1").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(content().string(first));

        mvc.perform(get("/sponsors/search").param("industry", "Idempotency").with(ip("10.1.0.3")))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        // mesma chave com outro corpo e erro do cliente
        mvc.perform(post("/sponsors").with(ip("10.1.0.3")).header("X-API-Key", USER_KEY)
                        .header("Idempotency-Key", "teste-idempotencia-1").contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Outro Nome\",\"industry\":\"Idempotency\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void rateLimitReturns429WithRetryAfter() throws Exception {
        for (int i = 0; i < 20; i++) {
            mvc.perform(get("/teams").with(ip("10.1.0.4"))).andExpect(status().isOk());
        }
        mvc.perform(get("/teams").with(ip("10.1.0.4")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("X-Rate-Limit-Remaining", "0"));

        // outro IP tem o proprio limite
        mvc.perform(get("/teams").with(ip("10.1.0.5"))).andExpect(status().isOk());

        // o IP estourado ainda passa se usar uma chave, porque a chave tem o proprio plano
        mvc.perform(get("/teams").with(ip("10.1.0.4")).header("X-API-Key", USER_KEY)).andExpect(status().isOk());
    }

    @Test
    void fakeForwardedForDoesNotResetTheLimit() throws Exception {
        // o cloudflare manda o IP de verdade, o X-Forwarded-For muda a cada requisicao como faria quem quer burlar
        for (int i = 0; i < 20; i++) {
            mvc.perform(get("/teams").header("CF-Connecting-IP", "203.0.113.7").header("X-Forwarded-For", "1.1.1." + i))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/teams").header("CF-Connecting-IP", "203.0.113.7").header("X-Forwarded-For", "9.9.9.9"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void eachPlanHasItsOwnLimit() throws Exception {
        mvc.perform(get("/teams").with(ip("10.1.0.10")))
                .andExpect(header().string("X-Rate-Limit-Limit", "20"));
        mvc.perform(get("/teams").with(ip("10.1.0.10")).header("X-API-Key", "chave-que-nao-existe"))
                .andExpect(header().string("X-Rate-Limit-Limit", "20"));
        mvc.perform(get("/teams").with(ip("10.1.0.10")).header("X-API-Key", USER_KEY))
                .andExpect(header().string("X-Rate-Limit-Limit", "100"));
        mvc.perform(get("/teams").with(ip("10.1.0.10")).header("X-API-Key", ADMIN_KEY))
                .andExpect(header().string("X-Rate-Limit-Limit", "1000"));
    }

    @Test
    void version2ChangesTheDriverFormat() throws Exception {
        // v1: HATEOAS e headers avisando que vai ser desligada
        mvc.perform(get("/drivers/1").with(ip("10.1.0.6")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links").exists())
                .andExpect(jsonPath("$.team.sponsors").exists())
                .andExpect(header().exists("Deprecation"));

        // v2: equipe resumida, idade calculada e sem HATEOAS
        mvc.perform(get("/drivers/1").with(ip("10.1.0.6")).header("API-Version", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.team.name").value("McLaren"))
                .andExpect(jsonPath("$.team.sponsors").doesNotExist())
                .andExpect(jsonPath("$.age").isNumber())
                .andExpect(jsonPath("$._links").doesNotExist());

        // versao que nao existe
        mvc.perform(get("/drivers/1").with(ip("10.1.0.6")).header("API-Version", "3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void corsOnlyAllowsTheConfiguredOrigins() throws Exception {
        mvc.perform(options("/sponsors").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mvc.perform(options("/sponsors").header("Origin", "http://site-malicioso.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validationErrorsListEachInvalidField() throws Exception {
        mvc.perform(post("/drivers").with(ip("10.1.0.7")).header("X-API-Key", USER_KEY)
                        .contentType(APPLICATION_JSON).content("{\"name\":\"X\",\"number\":150,\"team\":{\"id\":1}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.number").exists())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void invalidSortReturns400InsteadOf500() throws Exception {
        mvc.perform(get("/drivers").param("sort", "abc").with(ip("10.1.0.8")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid sort: No property 'abc' found for type 'Driver'"));
    }
}
