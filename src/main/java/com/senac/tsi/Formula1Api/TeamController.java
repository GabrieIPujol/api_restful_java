package com.senac.tsi.Formula1Api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

// endpoints das equipes, segue o mesmo molde do DriverController
@RestController
@Tag(name = "Teams", description = "Gerenciamento das equipes (construtores) de Formula 1")
public class TeamController {

    // exemplos do Swagger: o POST cria um registro novo e o PUT altera o id 1, que ja existe
    // (com o mesmo exemplo nos dois, o PUT dava 409 de valor repetido depois do POST)
    private static final String NEW_TEAM_EXAMPLE = """
            { "name": "Williams", "country": "United Kingdom", "base": "Grove", "foundedYear": 1977,
              "constructorTitles": 9, "sponsors": [ { "id": 2 } ] }""";

    private static final String TEAM_EXAMPLE = """
            { "name": "McLaren", "country": "United Kingdom", "base": "Woking", "foundedYear": 1963,
              "constructorTitles": 10, "sponsors": [ { "id": 1 } ] }""";

    private final TeamService service;
    private final TeamModelAssembler assembler;
    private final PagedResourcesAssembler<Team> pagedResourcesAssembler;
    private final PagedResourcesAssembler<Standing> standingsAssembler;

    // injecao de dependencia pelo construtor
    public TeamController(TeamService service,
                          TeamModelAssembler assembler,
                          PagedResourcesAssembler<Team> pagedResourcesAssembler,
                          PagedResourcesAssembler<Standing> standingsAssembler) {
        this.service = service;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.standingsAssembler = standingsAssembler;
    }

    // GET /teams - lista paginada
    @Operation(summary = "Get all teams", description = "Lista paginada de todas as equipes")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of teams")
    @GetMapping("/teams")
    public ResponseEntity<PagedModel<EntityModel<Team>>> getAllTeams(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }

    // GET /teams/{id} - 404 se nao achar
    @Operation(summary = "Get a team by its id", description = "Retorna uma equipe com os patrocinadores e os links HATEOAS")
    @ApiResponse(responseCode = "200", description = "Returns a valid team",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Team.class)))
    @ApiResponse(responseCode = "404", description = "Team not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping("/teams/{id}")
    public EntityModel<Team> getTeamById(@Parameter(description = "Id of the team", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    // POST /teams - cria a equipe ja com os patrocinadores
    @Operation(summary = "Creates a new team", description = "Os patrocinadores sao vinculados pelo id (Many-to-Many)")
    @ApiResponse(responseCode = "201", description = "Team created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "A referenced sponsor was not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "A team with this name already exists", content = @Content)
    @PostMapping("/teams")
    public ResponseEntity<EntityModel<Team>> newTeam(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New team",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Team.class),
                            examples = @ExampleObject(value = NEW_TEAM_EXAMPLE)))
            @RequestBody @Valid Team newTeam) {

        EntityModel<Team> entityModel = assembler.toModel(service.create(newTeam));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    // PUT /teams/{id} - substitui tudo, inclusive a lista de patrocinadores
    @Operation(summary = "Updates a team", description = "Substitui todos os dados da equipe, inclusive a lista de patrocinadores")
    @ApiResponse(responseCode = "200", description = "Team updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Team.class)))
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Team or sponsor not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "A team with this name already exists", content = @Content)
    @PutMapping("/teams/{id}")
    public ResponseEntity<EntityModel<Team>> updateTeam(
            @Parameter(description = "Id of the team", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated team",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Team.class),
                            examples = @ExampleObject(value = TEAM_EXAMPLE)))
            @RequestBody @Valid Team newTeam) {

        return ResponseEntity.ok(assembler.toModel(service.update(id, newTeam)));
    }

    // DELETE /teams/{id}
    @Operation(summary = "Deletes a team", description = "So e possivel excluir equipes sem pilotos e sem chefe de equipe vinculados")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a team", content = @Content)
    @ApiResponse(responseCode = "404", description = "Team not found, maybe it's already deleted", content = @Content)
    @ApiResponse(responseCode = "409", description = "Team still has drivers or a principal", content = @Content)
    @DeleteMapping("/teams/{id}")
    public ResponseEntity<Void> deleteTeam(@Parameter(description = "Id of the team (o 5 e a criada no POST de exemplo)", example = "5") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /teams/search?country= - equipes de um pais
    @Operation(summary = "Search teams by country", description = "Consulta personalizada: equipes de um pais (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of teams from the country")
    @GetMapping("/teams/search")
    public ResponseEntity<PagedModel<EntityModel<Team>>> getTeamsByCountry(
            @Parameter(description = "Country of the team", example = "Italy") @RequestParam String country,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.searchByCountry(country, pageable), assembler));
    }

    // GET /teams/sponsor/{sponsorId} - equipes de um patrocinador
    @Operation(summary = "Get teams of a sponsor", description = "Consulta personalizada: equipes patrocinadas por um patrocinador (Many-to-Many)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of teams of the sponsor")
    @ApiResponse(responseCode = "404", description = "Sponsor not found", content = @Content)
    @GetMapping("/teams/sponsor/{sponsorId}")
    public ResponseEntity<PagedModel<EntityModel<Team>>> getTeamsBySponsor(
            @Parameter(description = "Id of the sponsor", example = "1") @PathVariable Long sponsorId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findBySponsor(sponsorId, pageable), assembler));
    }

    // GET /teams/standings - tabela de construtores
    @Operation(summary = "Constructors' championship standings",
            description = "Consulta personalizada: tabela do campeonato de construtores, somando os pontos dos resultados de todos os pilotos da equipe, ordenada por pontos")
    @ApiResponse(responseCode = "200", description = "Returned the paginated standings table")
    // a tabela sempre ordena por pontos, entao no swagger so aparecem page e size (o sort seria ignorado)
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Zero-based page index (0..N)",
            schema = @Schema(type = "integer", defaultValue = "0"))
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "The size of the page to be returned",
            schema = @Schema(type = "integer", defaultValue = "10"))
    @GetMapping("/teams/standings")
    public ResponseEntity<PagedModel<EntityModel<Standing>>> getConstructorStandings(
            @Parameter(hidden = true) @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ResponseEntity.ok(standingsAssembler.toModel(service.standings(pageable), standing -> EntityModel.of(standing,
                linkTo(methodOn(TeamController.class).getTeamById(standing.id())).withRel("team"))));
    }
}
