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

// endpoints dos chefes de equipe, mesmo molde do DriverController
@RestController
@Tag(name = "Team Principals", description = "Gerenciamento dos chefes de equipe (relacionamento One-to-One com Team)")
public class TeamPrincipalController {

    // exemplos do Swagger: o POST cria um registro novo e o PUT altera o id 1, que ja existe
    // (com o mesmo exemplo nos dois, o PUT dava 409 de valor repetido depois do POST)
    private static final String NEW_PRINCIPAL_EXAMPLE = """
            { "name": "James Vowles", "nationality": "British", "since": 2023, "team": { "id": 5 } }""";

    private static final String PRINCIPAL_EXAMPLE = """
            { "name": "Andrea Stella", "nationality": "Italian", "since": 2023, "team": { "id": 1 } }""";

    private final TeamPrincipalService service;
    private final TeamPrincipalModelAssembler assembler;
    private final PagedResourcesAssembler<TeamPrincipal> pagedResourcesAssembler;

    // injecao de dependencia pelo construtor
    public TeamPrincipalController(TeamPrincipalService service,
                                   TeamPrincipalModelAssembler assembler,
                                   PagedResourcesAssembler<TeamPrincipal> pagedResourcesAssembler) {
        this.service = service;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    // GET /principals - lista paginada
    @Operation(summary = "Get all team principals", description = "Lista paginada de todos os chefes de equipe")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of team principals")
    @GetMapping("/principals")
    public ResponseEntity<PagedModel<EntityModel<TeamPrincipal>>> getAllPrincipals(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }

    // GET /principals/{id} - 404 se nao achar
    @Operation(summary = "Get a team principal by its id", description = "Retorna um chefe de equipe com a equipe e os links HATEOAS")
    @ApiResponse(responseCode = "200", description = "Returns a valid team principal",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = TeamPrincipal.class)))
    @ApiResponse(responseCode = "404", description = "Team principal not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping("/principals/{id}")
    public EntityModel<TeamPrincipal> getPrincipalById(@Parameter(description = "Id of the team principal", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    // POST /principals - se a equipe ja tiver chefe o banco barra e volta 409
    @Operation(summary = "Creates a new team principal", description = "A equipe e vinculada pelo id; cada equipe so pode ter um chefe (One-to-One). Nos dados iniciais as 4 equipes ja tem chefe, entao crie uma antes no POST /teams (ela vira o id 5)")
    @ApiResponse(responseCode = "201", description = "Team principal created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Referenced team not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "The team already has a principal", content = @Content)
    @PostMapping("/principals")
    public ResponseEntity<EntityModel<TeamPrincipal>> newPrincipal(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New team principal",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TeamPrincipal.class),
                            examples = @ExampleObject(value = NEW_PRINCIPAL_EXAMPLE)))
            @RequestBody @Valid TeamPrincipal newPrincipal) {

        EntityModel<TeamPrincipal> entityModel = assembler.toModel(service.create(newPrincipal));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    // PUT /principals/{id}
    @Operation(summary = "Updates a team principal", description = "Substitui todos os dados do chefe. A equipe e vinculada pelo id e nao pode ter outro chefe")
    @ApiResponse(responseCode = "200", description = "Team principal updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = TeamPrincipal.class)))
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Team principal or team not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "The team already has another principal", content = @Content)
    @PutMapping("/principals/{id}")
    public ResponseEntity<EntityModel<TeamPrincipal>> updatePrincipal(
            @Parameter(description = "Id of the team principal", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated team principal",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TeamPrincipal.class),
                            examples = @ExampleObject(value = PRINCIPAL_EXAMPLE)))
            @RequestBody @Valid TeamPrincipal newPrincipal) {

        return ResponseEntity.ok(assembler.toModel(service.update(id, newPrincipal)));
    }

    // DELETE /principals/{id} - o vinculo com a equipe e desfeito antes
    @Operation(summary = "Deletes a team principal", description = "Remove o chefe e desfaz o vinculo com a equipe")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a team principal", content = @Content)
    @ApiResponse(responseCode = "404", description = "Team principal not found, maybe it's already deleted", content = @Content)
    @DeleteMapping("/principals/{id}")
    public ResponseEntity<Void> deletePrincipal(@Parameter(description = "Id of the team principal (o 5 e o criado no POST de exemplo)", example = "5") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /principals/search?nationality= - chefes de uma nacionalidade
    @Operation(summary = "Search team principals by nationality", description = "Consulta personalizada: chefes de equipe de uma nacionalidade (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of team principals")
    @GetMapping("/principals/search")
    public ResponseEntity<PagedModel<EntityModel<TeamPrincipal>>> getPrincipalsByNationality(
            @Parameter(description = "Nationality", example = "Italian") @RequestParam String nationality,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.searchByNationality(nationality, pageable), assembler));
    }

    // GET /principals/team/{teamId} - o chefe de uma equipe, volta um so e nao uma pagina
    @Operation(summary = "Get the principal of a team", description = "Consulta personalizada: chefe de uma equipe (One-to-One)")
    @ApiResponse(responseCode = "200", description = "Returns the team principal",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = TeamPrincipal.class)))
    @ApiResponse(responseCode = "404", description = "Team not found or team has no principal", content = @Content)
    @GetMapping("/principals/team/{teamId}")
    public ResponseEntity<EntityModel<TeamPrincipal>> getPrincipalByTeam(
            @Parameter(description = "Id of the team", example = "1") @PathVariable Long teamId) {
        return ResponseEntity.ok(assembler.toModel(service.findByTeam(teamId)));
    }
}
