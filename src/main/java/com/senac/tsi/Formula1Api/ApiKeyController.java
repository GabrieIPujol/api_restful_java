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
@Tag(name = "API Keys", description = "Geracao e gerenciamento das chaves de API (header X-API-Key). Exige chave ADMIN, menos o /api-keys/me")
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
    @Operation(summary = "Get all API keys", description = "Lista paginada das chaves. A chave completa nunca aparece aqui, so o keyPrefix")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of API keys")
    @GetMapping("/api-keys")
    public ResponseEntity<PagedModel<EntityModel<ApiKey>>> getAllApiKeys(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }

    // GET /api-keys/{id}
    @Operation(summary = "Get an API key by its id", description = "Mostra nome, nivel, prefixo e se a chave esta ativa. A chave completa nao aparece")
    @ApiResponse(responseCode = "200", description = "Returns the API key",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = ApiKey.class)))
    @ApiResponse(responseCode = "404", description = "API key not found", content = @Content)
    @GetMapping("/api-keys/{id}")
    public EntityModel<ApiKey> getApiKeyById(@Parameter(description = "Id of the API key", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    // POST /api-keys - gera uma chave nova, ela so aparece nessa resposta
    @Operation(summary = "Generates a new API key",
            description = "Gera uma chave aleatoria. O campo key so aparece nesta resposta: guarde ele, depois nao da pra ver de novo")
    @ApiResponse(responseCode = "201", description = "API key created; the response body has the full key")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @PostMapping("/api-keys")
    public ResponseEntity<EntityModel<ApiKey>> newApiKey(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Owner and access level of the new key",
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
    @Operation(summary = "Updates an API key", description = "Altera nome, nivel e se a chave esta ativa. A chave em si nao muda")
    @ApiResponse(responseCode = "200", description = "API key updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = ApiKey.class)))
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "API key not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Tried to deactivate the key used in this request", content = @Content)
    @PutMapping("/api-keys/{id}")
    public ResponseEntity<EntityModel<ApiKey>> updateApiKey(
            @Parameter(description = "Id of the API key (o 3 e a criada no POST de exemplo; a 2 e a chave USER da configuracao)", example = "3") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated API key",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiKey.class),
                            examples = @ExampleObject(value = "{ \"name\": \"Front-end da equipe\", \"role\": \"USER\", \"active\": false }")))
            @RequestBody @Valid ApiKey newApiKey,
            @Parameter(hidden = true) @RequestAttribute(ApiKeyFilter.CURRENT_KEY) ApiKey current) {

        return ResponseEntity.ok(assembler.toModel(service.update(id, newApiKey, current.getId())));
    }

    // DELETE /api-keys/{id} - revoga a chave, ela para de funcionar na hora
    @Operation(summary = "Revokes an API key", description = "Apaga a chave, que para de funcionar na hora. Nao da pra revogar a propria chave usada na requisicao")
    @ApiResponse(responseCode = "204", description = "API key revoked", content = @Content)
    @ApiResponse(responseCode = "404", description = "API key not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Tried to revoke the key used in this request", content = @Content)
    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<Void> deleteApiKey(
            @Parameter(description = "Id of the API key (o 3 e a criada no POST de exemplo; a 2 e a chave USER da configuracao)", example = "3") @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(ApiKeyFilter.CURRENT_KEY) ApiKey current) {
        service.delete(id, current.getId());
        return ResponseEntity.noContent().build();
    }

    // GET /api-keys/role/{role} - consulta personalizada: chaves de um nivel
    @Operation(summary = "Get API keys by role", description = "Consulta personalizada: chaves de um nivel de acesso (USER ou ADMIN)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of API keys")
    @ApiResponse(responseCode = "400", description = "Invalid role", content = @Content)
    @GetMapping("/api-keys/role/{role}")
    public ResponseEntity<PagedModel<EntityModel<ApiKey>>> getApiKeysByRole(
            @Parameter(description = "Access level", example = "USER") @PathVariable ApiKeyRole role,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByRole(role, pageable), assembler));
    }

    // GET /api-keys/me - mostra de quem e a chave que veio no header (aceita USER e ADMIN)
    @Operation(summary = "Shows the API key used in this request", description = "Consulta personalizada: de quem e a chave enviada e qual o nivel dela")
    @ApiResponse(responseCode = "200", description = "Returns the current API key",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = ApiKey.class)))
    @GetMapping("/api-keys/me")
    public EntityModel<ApiKey> getCurrentApiKey(@Parameter(hidden = true) @RequestAttribute(ApiKeyFilter.CURRENT_KEY) ApiKey current) {
        return EntityModel.of(current,
                linkTo(methodOn(ApiKeyController.class).getCurrentApiKey(null)).withSelfRel(),
                linkTo(methodOn(ApiKeyController.class).getApiKeyById(current.getId())).withRel("apiKey"));
    }
}
