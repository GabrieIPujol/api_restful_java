package com.senac.tsi.Formula1Api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

// geracao e gerenciamento das chaves de API
// o ApiKeyFilter so deixa chegar aqui quem manda uma chave ADMIN (menos o /api-keys/me, que aceita qualquer chave)
@RestController
@Tag(name = "Chaves de API", description = "Criacao e controle das chaves usadas no header X-API-Key. "
        + "Todas as rotas exigem uma chave ADMIN, menos o /api-keys/me, que aceita qualquer chave")
public class ApiKeyController {

    // exemplo que aparece no Swagger
    private static final String API_KEY_EXAMPLE = """
            { "name": "Front-end da equipe", "role": "USER" }""";

    private final ApiKeyService service;
    private final ApiKeyModelAssembler assembler;
    private final PagedResourcesAssembler<ApiKey> pagedResourcesAssembler;

    // injecao de dependencia pelo construtor
    public ApiKeyController(ApiKeyService service,
                            ApiKeyModelAssembler assembler,
                            PagedResourcesAssembler<ApiKey> pagedResourcesAssembler) {
        this.service = service;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    // GET /api-keys - lista paginada (a chave em si nunca aparece, so o comeco dela)
    @Operation(summary = "Lista as chaves de API", description = "Lista paginada das chaves com nome, nivel (role), se esta ativa "
            + "e o keyPrefix, o comeco da chave para identificar. A chave completa nunca aparece aqui")
    @ApiResponse(responseCode = "200", description = "Lista paginada de chaves")
    @GetMapping("/api-keys")
    public ResponseEntity<PagedModel<EntityModel<ApiKey>>> getAllApiKeys(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }

    // GET /api-keys/{id}
    @Operation(summary = "Busca uma chave de API pelo id", description = "Mostra nome, nivel, prefixo e se a chave esta ativa. "
            + "A chave completa nao aparece, porque o banco so guarda o hash dela")
    @ApiResponse(responseCode = "200", description = "Chave encontrada",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = ApiKey.class)))
    @ApiResponse(responseCode = "404", description = "Chave nao encontrada - nao existe chave com esse id "
            + "(nunca foi criada ou ja foi revogada)", content = @Content)
    @GetMapping("/api-keys/{id}")
    public EntityModel<ApiKey> getApiKeyById(@Parameter(description = "Id da chave", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    // POST /api-keys - gera uma chave nova, ela so aparece nessa resposta
    @Operation(summary = "Gera uma chave de API", description = "Cria uma chave aleatoria para alguem usar a API. "
            + "Informe o dono (name) e o nivel (role): USER le e altera os dados, ADMIN tambem gerencia as chaves. "
            + "A chave completa vem no campo key so nesta resposta: guarde ela, depois nao da para ver de novo")
    @ApiResponse(responseCode = "201", description = "Chave criada; o campo key da resposta traz a chave completa")
    @ApiResponse(responseCode = "400", description = "Dados invalidos - name (3 a 100 caracteres) e role (USER ou ADMIN) "
            + "sao obrigatorios. Tambem acontece com campo do tipo errado ou JSON mal formado; "
            + "a resposta diz qual campo falhou e por que", content = @Content)
    @PostMapping("/api-keys")
    public ResponseEntity<EntityModel<ApiKey>> newApiKey(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dono e nivel de acesso da chave nova",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiKey.class),
                            examples = @ExampleObject(value = API_KEY_EXAMPLE)))
            @RequestBody @Valid ApiKey newApiKey) {

        EntityModel<ApiKey> entityModel = assembler.toModel(service.create(newApiKey));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    // PUT /api-keys/{id} - muda nome, nivel ou desativa (active: false)
    @Operation(summary = "Atualiza uma chave de API", description = "Altera o nome, o nivel e se a chave esta ativa. "
            + "Com active false a chave para de funcionar, mas continua cadastrada e pode ser reativada. A chave em si nao muda")
    @ApiResponse(responseCode = "200", description = "Chave atualizada",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = ApiKey.class)))
    @ApiResponse(responseCode = "400", description = "Dados invalidos - o id nao e um numero inteiro, ou algum campo do JSON "
            + "e invalido (mesmas regras do POST)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Chave nao encontrada - nao existe chave com esse id", content = @Content)
    @ApiResponse(responseCode = "409", description = "Nao da para desativar a propria chave - essa e a chave usada nesta requisicao; "
            + "use outra chave ADMIN", content = @Content)
    @PutMapping("/api-keys/{id}")
    public ResponseEntity<EntityModel<ApiKey>> updateApiKey(
            @Parameter(description = "Id da chave (o 3 e a criada no POST de exemplo; a 2 e a chave USER da configuracao)", example = "3") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados da chave",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiKey.class),
                            examples = @ExampleObject(value = "{ \"name\": \"Front-end da equipe\", \"role\": \"USER\", \"active\": false }")))
            @RequestBody @Valid ApiKey newApiKey,
            @Parameter(hidden = true) @RequestAttribute(ApiKeyFilter.CURRENT_KEY) ApiKey current) {

        return ResponseEntity.ok(assembler.toModel(service.update(id, newApiKey, current.getId())));
    }

    // DELETE /api-keys/{id} - revoga a chave, ela para de funcionar na hora
    @Operation(summary = "Revoga uma chave de API", description = "Apaga a chave de vez: ela para de funcionar na hora e nao pode "
            + "ser reativada (para pausar, use o PUT com active false). Nao da para revogar a chave usada nesta requisicao")
    @ApiResponse(responseCode = "204", description = "Chave revogada", content = @Content)
    @ApiResponse(responseCode = "404", description = "Chave nao encontrada - nao existe chave com esse id "
            + "(talvez ja tenha sido revogada)", content = @Content)
    @ApiResponse(responseCode = "409", description = "Nao da para revogar a propria chave - essa e a chave usada nesta requisicao; "
            + "use outra chave ADMIN", content = @Content)
    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<Void> deleteApiKey(
            @Parameter(description = "Id da chave (o 3 e a criada no POST de exemplo; a 2 e a chave USER da configuracao)", example = "3") @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(ApiKeyFilter.CURRENT_KEY) ApiKey current) {
        service.delete(id, current.getId());
        return ResponseEntity.noContent().build();
    }

    // GET /api-keys/role/{role} - consulta personalizada: chaves de um nivel
    @Operation(summary = "Lista as chaves por nivel", description = "Consulta personalizada: chaves de um nivel de acesso, USER ou ADMIN")
    @ApiResponse(responseCode = "200", description = "Lista paginada das chaves desse nivel")
    @ApiResponse(responseCode = "400", description = "Nivel invalido - use USER ou ADMIN, em maiusculas", content = @Content)
    @GetMapping("/api-keys/role/{role}")
    public ResponseEntity<PagedModel<EntityModel<ApiKey>>> getApiKeysByRole(
            @Parameter(description = "Nivel de acesso: USER ou ADMIN", example = "USER") @PathVariable ApiKeyRole role,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByRole(role, pageable), assembler));
    }

    // GET /api-keys/me - mostra de quem e a chave que veio no header (aceita USER e ADMIN)
    @Operation(summary = "Mostra a chave usada na requisicao", description = "Consulta personalizada: de quem e a chave enviada "
            + "no X-API-Key e qual o nivel dela. Aceita qualquer chave valida, USER ou ADMIN; serve para conferir se a chave funciona")
    @ApiResponse(responseCode = "200", description = "Dados da chave enviada",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = ApiKey.class)))
    @GetMapping("/api-keys/me")
    public EntityModel<ApiKey> getCurrentApiKey(@Parameter(hidden = true) @RequestAttribute(ApiKeyFilter.CURRENT_KEY) ApiKey current) {
        return EntityModel.of(current,
                linkTo(methodOn(ApiKeyController.class).getCurrentApiKey(null)).withSelfRel(),
                linkTo(methodOn(ApiKeyController.class).getApiKeyById(current.getId())).withRel("apiKey"));
    }
}
