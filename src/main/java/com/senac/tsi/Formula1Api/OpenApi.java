package com.senac.tsi.Formula1Api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// informacoes que aparecem no topo do Swagger e os documentos de cada versao da api
@Configuration
public class OpenApi {

    private static final String API_KEY_SCHEME = "ApiKey";

    // ordem dos grupos no Swagger: a chave primeiro, depois cada cadastro antes de quem depende dele
    // (patrocinador -> equipe -> chefe e piloto -> resultado). os nomes tem que bater com o @Tag dos controllers
    static final List<String> TAG_ORDER = List.of("Chaves de API", "Inicio", "Patrocinadores", "Equipes",
            "Chefes de equipe", "Pilotos", "Pilotos v2", "Resultados");

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
                                .forEach(parameter -> parameter
                                        .description("Versao da API: sem o header vale a 1, e a 2 muda o formato dos pilotos. "
                                                + "Neste documento ja vem a " + version)
                                        .schema(new StringSchema()._default(version)._enum(List.of(version))));
                        // o springdoc descreve page, size e sort em ingles, aqui troca pelo texto em portugues
                        operation.getParameters().stream()
                                .filter(parameter -> "query".equals(parameter.getIn()))
                                .forEach(parameter -> {
                                    switch (parameter.getName()) {
                                        case "page" -> parameter.description("Numero da pagina, comecando em 0");
                                        case "size" -> parameter.description("Quantos registros por pagina");
                                        case "sort" -> parameter.description("Ordenacao no formato campo,asc ou campo,desc "
                                                + "(ex.: name,asc); repita o sort para ordenar por mais de um campo");
                                        default -> { }
                                    }
                                });
                    }
                    documentFilters(operation, handlerMethod);
                    return operation;
                })
                .addOpenApiCustomizer(openApi -> {
                    // o Swagger mostra os grupos na ordem da lista tags, entao ordena ela pelo TAG_ORDER
                    openApi.setTags(openApi.getTags().stream()
                            .sorted(Comparator.comparingInt(tag -> TAG_ORDER.indexOf(tag.getName())))
                            .toList());
                    documentResponses(openApi, version);
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
            operation.getResponses().addApiResponse("401", new ApiResponse().description("Sem chave ou chave invalida - mande uma chave "
                    + "ativa no header X-API-Key (no Swagger, clique em Authorize); chave errada, desativada ou revogada tambem cai aqui. "
                    + "Chaves novas sao geradas por um ADMIN no POST /api-keys"));
            // o /api-keys/me aceita qualquer chave, entao nele nao tem 403
            if (keysEndpoint && !List.of(mapping.path()).contains("/api-keys/me"))
                operation.getResponses().addApiResponse("403", new ApiResponse().description("Chave sem permissao - essa rota exige uma chave ADMIN"));
        } else {
            // leitura e livre, mas a chave e opcional: o "{}" quer dizer "sem chave tambem vale"
            // assim, depois do Authorize, o Swagger manda a chave nos GET e vale o limite do plano
            operation.addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
            operation.addSecurityItem(new SecurityRequirement());
            operation.getResponses().addApiResponse("401", new ApiResponse().description("Chave invalida - sem chave a leitura e livre, "
                    + "mas se o header X-API-Key for enviado ele precisa ser de uma chave ativa"));
        }

        // todo POST aceita Idempotency-Key
        if (methods.contains(RequestMethod.POST)) {
            operation.addParametersItem(new HeaderParameter()
                    .name(IdempotencyFilter.HEADER)
                    .required(false)
                    .description("Opcional. Codigo unico da operacao (ex.: um UUID): se o mesmo POST for reenviado com o mesmo codigo, "
                            + "a API devolve a resposta da primeira vez em vez de criar outro registro")
                    .schema(new StringSchema().example("3f2b8c1e-9d4a-4c7e-b1a2-6e5f0d9c8a71")));
            operation.getResponses().addApiResponse("422", new ApiResponse().description("Idempotency-Key repetida com outro corpo - "
                    + "esse codigo ja foi usado em um POST diferente; gere um codigo novo"));
        }

        // 400 so com os motivos que existem nessa rota (API-Version que nao existe da 400 em todas)
        if (!operation.getResponses().containsKey("400")) {
            List<Parameter> parameters = operation.getParameters() == null ? List.of() : operation.getParameters();
            List<String> reasons = new ArrayList<>();
            if (parameters.stream().anyMatch(parameter -> "path".equals(parameter.getIn())))
                reasons.add("valor errado na URL (ex.: id que nao e numero)");
            if (parameters.stream().anyMatch(parameter -> "query".equals(parameter.getIn()) && Boolean.TRUE.equals(parameter.getRequired())))
                reasons.add("faltou o parametro obrigatorio");
            if (parameters.stream().anyMatch(parameter -> "sort".equals(parameter.getName())))
                reasons.add("sort com um campo que nao existe (ex.: sort=abc)");
            if (operation.getRequestBody() != null)
                reasons.add("corpo invalido");
            reasons.add("API-Version que nao existe (so 1 e 2)");
            operation.getResponses().addApiResponse("400", new ApiResponse().description("Requisicao invalida - " + String.join("; ", reasons)));
        }
        if (operation.getRequestBody() != null)
            operation.getResponses().addApiResponse("415", new ApiResponse().description("Corpo que nao e JSON - mande o corpo em JSON "
                    + "com o header Content-Type: application/json"));

        // qualquer endpoint pode bater no rate limit
        operation.getResponses().addApiResponse("429", new ApiResponse().description("Limite do plano estourado - espere os segundos "
                + "do header Retry-After e tente de novo"));
    }

    // ---- exemplos de erro e headers de resposta, com as mensagens que a api devolve de verdade ----
    // o primeiro pedaco do caminho diz de qual recurso e a rota (/drivers/{id} -> drivers)

    // nome usado nas mensagens de 404 ("Could not find driver with ID: 99")
    private static final Map<String, String> RESOURCE_NAMES = Map.of("teams", "team", "drivers", "driver",
            "sponsors", "sponsor", "results", "race result", "principals", "team principal", "api-keys", "API key");
    // nome da classe que aparece no 400 de sort invalido
    private static final Map<String, String> RESOURCE_TYPES = Map.of("teams", "Team", "drivers", "Driver",
            "sponsors", "Sponsor", "results", "RaceResult", "principals", "TeamPrincipal", "api-keys", "ApiKey");
    // no POST o 404 e do registro ligado pelo id (piloto -> equipe, resultado -> piloto...)
    private static final Map<String, String> REFERENCED = Map.of("teams", "sponsor", "drivers", "team",
            "principals", "team", "results", "driver");
    // os 409 que tem mensagem propria; o resto e valor repetido num campo unique
    private static final Map<String, String> CONFLICTS = Map.of(
            "DELETE /drivers/{id}", "Driver still has race results",
            "DELETE /teams/{id}", "Team still has drivers or a principal",
            "PUT /api-keys/{id}", "You cannot deactivate the API key you are using",
            "DELETE /api-keys/{id}", "You cannot revoke the API key you are using");
    // um exemplo de campo invalido por recurso, com a mensagem que o Bean Validation devolve em portugues
    // (o Swagger manda Accept-Language do navegador, entao em pt-BR vem assim)
    private static final Map<String, Map<String, String>> FIELD_ERRORS = Map.of(
            "teams", Map.of("name", "tamanho deve ser entre 2 e 100", "foundedYear", "deve ser maior que ou igual à 1900"),
            "drivers", Map.of("name", "tamanho deve ser entre 3 e 100", "number", "deve ser menor que ou igual à 99"),
            "principals", Map.of("name", "não deve estar em branco", "since", "deve ser maior que ou igual à 1950"),
            "sponsors", Map.of("name", "não deve estar em branco", "industry", "não deve estar em branco"),
            "results", Map.of("position", "deve ser menor que ou igual à 30", "status", "não deve ser nulo"),
            "api-keys", Map.of("name", "tamanho deve ser entre 3 e 100", "role", "não deve ser nulo"));

    private static void documentResponses(OpenAPI openApi, String version) {
        // o formato de todos os erros, os exemplos apontam pra ele
        openApi.getComponents().addSchemas("ProblemDetail", new ObjectSchema()
                .description("Formato de todos os erros da API (RFC 9457)")
                .addProperty("title", new StringSchema().description("Nome do status HTTP"))
                .addProperty("status", new IntegerSchema().description("Codigo HTTP"))
                .addProperty("detail", new StringSchema().description("O que deu errado"))
                .addProperty("instance", new StringSchema().description("Caminho da requisicao"))
                .addProperty("errors", new ObjectSchema().additionalProperties(new StringSchema())
                        .description("So no 400 de validacao: cada campo invalido e o motivo")));

        openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) ->
                operation.getResponses().forEach((code, response) -> {
                    int status = Integer.parseInt(code);
                    if (status >= 400)
                        response.content(new Content().addMediaType("application/problem+json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                                .example(problemExample(status, path, method, operation))));
                    addHeaders(response, status, path, version);
                })));
    }

    private static Map<String, Object> problemExample(int status, String path, PathItem.HttpMethod method, Operation operation) {
        String resource = path.split("/").length > 1 ? path.split("/")[1] : "";
        String pathVar = path.contains("{") ? path.substring(path.indexOf('{') + 1, path.indexOf('}')) : null;
        List<Parameter> parameters = operation.getParameters() == null ? List.of() : operation.getParameters();
        Optional<Parameter> requiredQuery = parameters.stream()
                .filter(parameter -> "query".equals(parameter.getIn()) && Boolean.TRUE.equals(parameter.getRequired()))
                .findFirst();

        String instance = path.replaceAll("\\{[^}]+}", "1");
        Map<String, String> errors = null;
        String detail;
        // no 400 mostra o motivo mais comum da rota: corpo, valor na URL, parametro faltando, sort e por ultimo a versao
        if (status == 400 && operation.getRequestBody() != null) {
            detail = "One or more fields are invalid";
            errors = FIELD_ERRORS.get(resource);
        } else if (status == 400 && pathVar != null) {
            detail = "Invalid value 'abc' for parameter '" + pathVar + "'";
            instance = path.replaceAll("\\{[^}]+}", "abc");
        } else if (status == 400 && requiredQuery.isPresent()) {
            detail = "Required parameter '" + requiredQuery.get().getName() + "' is not present.";
        } else if (status == 400 && parameters.stream().anyMatch(parameter -> "sort".equals(parameter.getName()))) {
            detail = "Invalid sort: No property 'abc' found for type '" + RESOURCE_TYPES.get(resource) + "'";
        } else if (status == 400) {
            detail = "Invalid API version: '3.0.0'.";
        } else if (status == 401) {
            detail = path.startsWith("/api-keys") ? "Missing X-API-Key header: /api-keys requires an API key"
                    : method == PathItem.HttpMethod.GET ? "Invalid or revoked API key"
                    : "Missing X-API-Key header: POST, PUT and DELETE require an API key";
        } else if (status == 403) {
            detail = "This operation requires an ADMIN API key";
        } else if (status == 404) {
            // /drivers/team/{teamId} -> team, /drivers/{id} -> driver, POST /drivers -> team (o registro ligado)
            String entity = pathVar != null && !pathVar.equals("id") ? RESOURCE_NAMES.get(pathVar.replace("Id", "s"))
                    : pathVar != null ? RESOURCE_NAMES.get(resource) : REFERENCED.get(resource);
            detail = "Could not find " + entity + " with ID: 99";
            instance = path.replaceAll("\\{[^}]+}", "99");
        } else if (status == 409) {
            detail = CONFLICTS.getOrDefault(method + " " + path,
                    "Operation conflicts with existing data (duplicated value or record still referenced)");
        } else if (status == 415) {
            detail = "Content-Type 'text/plain;charset=UTF-8' is not supported.";
        } else if (status == 422) {
            detail = "Idempotency-Key was already used with a different request body";
        } else if (status == 429) {
            detail = "Too many requests, try again in 23s";
        } else {
            detail = "Unexpected error, try again later";
        }

        Map<String, Object> example = new LinkedHashMap<>();
        example.put("title", HttpStatus.valueOf(status).getReasonPhrase());
        example.put("status", status);
        example.put("detail", detail);
        example.put("instance", instance);
        if (errors != null)
            example.put("errors", errors);
        return example;
    }

    // headers que os filtros poem na resposta
    private static void addHeaders(ApiResponse response, int status, String path, String version) {
        if (status >= 200 && status < 300) {
            response.addHeaderObject("X-Rate-Limit-Limit", header("Requisicoes por minuto do seu plano", new IntegerSchema()));
            response.addHeaderObject("X-Rate-Limit-Remaining", header("Quantas requisicoes ainda restam neste minuto", new IntegerSchema()));
            // na v1 tudo que comeca com /drivers avisa que vai ser desligado
            if (version.equals("1") && path.startsWith("/drivers")) {
                response.addHeaderObject("Deprecation", header("A v1 dos pilotos esta obsoleta desde esta data", new StringSchema()));
                response.addHeaderObject("Sunset", header("Data em que a v1 dos pilotos deve ser desligada", new StringSchema()));
            }
        }
        if (status == 201) {
            response.addHeaderObject("Location", header("Endereco do registro criado", new StringSchema()));
            response.addHeaderObject("Idempotent-Replayed", header("Vem com true quando a resposta e a repeticao "
                    + "de um POST com a mesma Idempotency-Key", new StringSchema()));
        }
        if (status == 429)
            response.addHeaderObject("Retry-After", header("Em quantos segundos da para tentar de novo", new IntegerSchema()));
    }

    private static Header header(String description, Schema<?> schema) {
        return new Header().description(description).schema(schema);
    }
}
