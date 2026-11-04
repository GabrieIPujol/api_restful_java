package com.senac.tsi.Formula1Api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// informacoes que aparecem no topo do Swagger e os documentos de cada versao da api
@Configuration
public class OpenApi {

    private static final String API_KEY_SCHEME = "ApiKey";

    @Bean
    public OpenAPI customOpenAPI(@Value("${app.rate-limit.requests-per-minute}") long visitorLimit,
                                 @Value("${app.rate-limit.user-requests-per-minute}") long userLimit,
                                 @Value("${app.rate-limit.admin-requests-per-minute}") long adminLimit) {
        return new OpenAPI()
                .info(new Info()
                        .title("Formula 1 API")
                        .version("2.0.2")
                        .description("API RESTful para gerenciar equipes, chefes de equipe, pilotos, patrocinadores " +
                                "e resultados de corrida da Formula 1, com tabelas de classificacao de pilotos e construtores. " +
                                "Todas as listagens sao paginadas (`page`, `size`, `sort`).\n\n" +
                                "**HATEOAS (HAL):** cada resposta traz um bloco `_links` com os enderecos do que da para fazer em seguida " +
                                "(ver, alterar, apagar, ir para os registros relacionados ou para a proxima pagina). " +
                                "Assim o cliente navega pela API seguindo os links, sem montar URL na mao. " +
                                "Comece pelo `GET /`, que lista todos os recursos. A v2 dos pilotos responde sem `_links`.\n\n" +
                                "**Chave de API:** GET e publico. POST, PUT e DELETE exigem o header `X-API-Key` (clique em Authorize). " +
                                "Gerenciar chaves em `/api-keys` exige uma chave ADMIN.\n\n" +
                                "**Idempotencia:** evita registro duplicado quando o mesmo POST e enviado duas vezes " +
                                "(por exemplo, a internet caiu e o cliente reenviou). Mande no POST o header `Idempotency-Key` " +
                                "com um codigo unico. Se chegar outro POST com o mesmo codigo, a API nao cria de novo: " +
                                "devolve a resposta da primeira vez.\n\n" +
                                "**Planos (rate limit):** cada plano tem um limite de requisicoes por minuto. Passou dele, a resposta e `429`, " +
                                "e o header `Retry-After` diz em quantos segundos tentar de novo. O header `X-Rate-Limit-Limit` mostra o limite do seu plano.\n\n" +
                                "| Plano | Como acessar | O que pode | Limite por minuto | Contado por |\n" +
                                "|---|---|---|---|---|\n" +
                                "| Visitante | Sem chave | Ler os dados (GET) | " + visitorLimit + " | IP |\n" +
                                "| USER | Chave USER no `X-API-Key` | Ler e alterar os dados (POST, PUT, DELETE) | " + userLimit + " | Chave |\n" +
                                "| ADMIN | Chave ADMIN no `X-API-Key` | Tudo isso e gerenciar as chaves em `/api-keys` | " + adminLimit + " | Chave |\n\n" +
                                "**Versoes:** escolha no seletor acima. A v2 muda os pilotos e e pedida com o header `API-Version: 2`.")
                        // as duas paginas ficam em src/main/resources/static, o proprio spring serve elas
                        .termsOfService("/terms.html")
                        .license(new License().name("MIT").url("/license.txt"))
                        .contact(new Contact().name("Gabriel Pujol")
                                .url("https://gabrielpujol.com.br")
                                .email("gabriel.pagvelosa@gmail.com")))
                // cria o botao Authorize do Swagger pra colar a chave
                .components(new Components().addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(ApiKeyFilter.HEADER)
                        .description("Chave de API USER ou ADMIN")));
    }

    // um documento do Swagger por versao (escolhe no "Select a definition" do topo)
    // um documento so nao da conta porque a v1 e a v2 usam o mesmo caminho e metodo
    @Bean
    public GroupedOpenApi v1Api() {
        return versionGroup("1");
    }

    @Bean
    public GroupedOpenApi v2Api() {
        return versionGroup("2");
    }

    private static GroupedOpenApi versionGroup(String version) {
        return GroupedOpenApi.builder()
                .group("v" + version)
                .displayName("Formula 1 API v" + version)
                // mostra os endpoints dessa versao mais os que nao tem versao (servem as duas)
                .addOpenApiMethodFilter(method -> {
                    RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                    return mapping == null || mapping.version().isEmpty() || mapping.version().equals(version);
                })
                .addOperationCustomizer((operation, handlerMethod) -> {
                    // ja deixa o header API-Version preenchido com a versao do documento
                    if (operation.getParameters() != null) {
                        operation.getParameters().stream()
                                .filter(parameter -> ApiVersionConfig.HEADER.equals(parameter.getName()))
                                .forEach(parameter -> parameter.schema(new StringSchema()._default(version)._enum(List.of(version))));
                    }
                    documentFilters(operation, handlerMethod);
                    return operation;
                })
                .build();
    }

    // documenta o que os filtros fazem em cada endpoint, assim nao precisa repetir anotacao em todo metodo
    private static void documentFilters(Operation operation, HandlerMethod handlerMethod) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequestMapping.class);
        Set<RequestMethod> methods = mapping == null ? Set.of() : Set.of(mapping.method());
        boolean read = methods.equals(Set.of(RequestMethod.GET));
        boolean keysEndpoint = handlerMethod.getBeanType().equals(ApiKeyController.class);

        // escrita e /api-keys pedem chave: ganha o cadeado no Swagger e os erros 401/403
        if (!read || keysEndpoint) {
            operation.addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
            operation.getResponses().addApiResponse("401", new ApiResponse().description("Missing, invalid or revoked X-API-Key"));
            // o /api-keys/me aceita qualquer chave, entao nele nao tem 403
            if (keysEndpoint && !List.of(mapping.path()).contains("/api-keys/me"))
                operation.getResponses().addApiResponse("403", new ApiResponse().description("This operation requires an ADMIN API key"));
        } else {
            // leitura e livre, mas a chave e opcional: o "{}" quer dizer "sem chave tambem vale"
            // assim, depois do Authorize, o Swagger manda a chave nos GET e vale o limite do plano
            operation.addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
            operation.addSecurityItem(new SecurityRequirement());
            operation.getResponses().addApiResponse("401", new ApiResponse().description("Invalid or revoked X-API-Key (without a key, reading is free)"));
        }

        // todo POST aceita Idempotency-Key
        if (methods.contains(RequestMethod.POST)) {
            operation.addParametersItem(new HeaderParameter()
                    .name(IdempotencyFilter.HEADER)
                    .required(false)
                    .description("Chave unica (ex.: um UUID) que deixa repetir o POST sem duplicar: repetir a chave devolve a resposta original")
                    .schema(new StringSchema().example("3f2b8c1e-9d4a-4c7e-b1a2-6e5f0d9c8a71")));
            operation.getResponses().addApiResponse("422", new ApiResponse().description("Idempotency-Key was already used with a different request body"));
        }

        // 400 so com os motivos que existem nessa rota (API-Version que nao existe da 400 em todas)
        if (!operation.getResponses().containsKey("400")) {
            List<Parameter> parameters = operation.getParameters() == null ? List.of() : operation.getParameters();
            List<String> reasons = new ArrayList<>();
            if (parameters.stream().anyMatch(parameter -> "path".equals(parameter.getIn())))
                reasons.add("wrong value in the URL");
            if (parameters.stream().anyMatch(parameter -> "query".equals(parameter.getIn()) && Boolean.TRUE.equals(parameter.getRequired())))
                reasons.add("missing required parameter");
            if (parameters.stream().anyMatch(parameter -> "sort".equals(parameter.getName())))
                reasons.add("invalid sort");
            if (operation.getRequestBody() != null)
                reasons.add("invalid body");
            reasons.add("unsupported API-Version");
            operation.getResponses().addApiResponse("400", new ApiResponse().description("Invalid request: " + String.join(", ", reasons)));
        }
        if (operation.getRequestBody() != null)
            operation.getResponses().addApiResponse("415", new ApiResponse().description("Content-Type must be application/json"));

        // qualquer endpoint pode bater no rate limit
        operation.getResponses().addApiResponse("429", new ApiResponse().description("Too many requests, wait the seconds in the Retry-After header"));
    }
}
