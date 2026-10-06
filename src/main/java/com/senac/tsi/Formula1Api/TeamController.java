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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@Tag(name = "Teams", description = "Gerenciamento das equipes (construtores) de Formula 1")
public class TeamController {

    private static final String TEAM_EXAMPLE = """
            { "name": "McLaren", "country": "United Kingdom", "base": "Woking", "foundedYear": 1963,
              "constructorTitles": 10, "sponsors": [ { "id": 1 } ] }""";

    private final TeamRepository repository;
    private final SponsorRepository sponsorRepository;
    private final TeamModelAssembler assembler;
    private final PagedResourcesAssembler<Team> pagedResourcesAssembler;
    private final PagedResourcesAssembler<Standing> standingsAssembler;

    public TeamController(TeamRepository repository,
                          SponsorRepository sponsorRepository,
                          TeamModelAssembler assembler,
                          PagedResourcesAssembler<Team> pagedResourcesAssembler,
                          PagedResourcesAssembler<Standing> standingsAssembler) {
        this.repository = repository;
        this.sponsorRepository = sponsorRepository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.standingsAssembler = standingsAssembler;
    }

    @Operation(summary = "Get all teams", description = "Lista paginada de todas as equipes")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of teams")
    @GetMapping("/teams")
    public ResponseEntity<PagedModel<EntityModel<Team>>> getAllTeams(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Team> teamPage = repository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(teamPage, assembler));
    }

    @Operation(summary = "Get a team by its id")
    @ApiResponse(responseCode = "200", description = "Returns a valid team",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Team.class)))
    @ApiResponse(responseCode = "404", description = "Team not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping("/teams/{id}")
    public EntityModel<Team> getTeamById(@Parameter(description = "Id of the team", example = "1") @PathVariable Long id) {
        Team team = repository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));
        return assembler.toModel(team);
    }

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
                            examples = @ExampleObject(value = TEAM_EXAMPLE)))
            @RequestBody @Valid Team newTeam) {

        newTeam.setSponsors(resolveSponsors(newTeam.getSponsors()));
        EntityModel<Team> entityModel = assembler.toModel(repository.save(newTeam));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    @Operation(summary = "Updates a team", description = "Substitui todos os dados da equipe, inclusive a lista de patrocinadores")
    @ApiResponse(responseCode = "200", description = "Team updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Team.class)))
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Team or sponsor not found", content = @Content)
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

        Team updated = repository.findById(id)
                .map(team -> {
                    team.setName(newTeam.getName());
                    team.setCountry(newTeam.getCountry());
                    team.setBase(newTeam.getBase());
                    team.setFoundedYear(newTeam.getFoundedYear());
                    team.setConstructorTitles(newTeam.getConstructorTitles());
                    team.setSponsors(resolveSponsors(newTeam.getSponsors()));
                    return repository.save(team);
                })
                .orElseThrow(() -> new TeamNotFoundException(id));

        return ResponseEntity.ok(assembler.toModel(updated));
    }

    @Operation(summary = "Deletes a team", description = "So e possivel excluir equipes sem pilotos e sem chefe de equipe vinculados")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a team", content = @Content)
    @ApiResponse(responseCode = "404", description = "Team not found, maybe it's already deleted", content = @Content)
    @ApiResponse(responseCode = "409", description = "Team still has drivers or a principal", content = @Content)
    @DeleteMapping("/teams/{id}")
    public ResponseEntity<?> deleteTeam(@Parameter(description = "Id of the team", example = "1") @PathVariable Long id) {
        var team = repository.findById(id);
        if (team.isEmpty())
            return ResponseEntity.notFound().build();

        if (!team.get().getDrivers().isEmpty() || team.get().getPrincipal() != null)
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Team still has drivers or a principal");

        repository.delete(team.get());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search teams by country", description = "Consulta personalizada: equipes de um pais (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of teams from the country")
    @GetMapping("/teams/search")
    public ResponseEntity<PagedModel<EntityModel<Team>>> getTeamsByCountry(
            @Parameter(description = "Country of the team", example = "Italy") @RequestParam String country,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Team> teamPage = repository.findByCountryIgnoreCase(country, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(teamPage, assembler));
    }

    @Operation(summary = "Get teams of a sponsor", description = "Consulta personalizada: equipes patrocinadas por um patrocinador (Many-to-Many)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of teams of the sponsor")
    @ApiResponse(responseCode = "404", description = "Sponsor not found", content = @Content)
    @GetMapping("/teams/sponsor/{sponsorId}")
    public ResponseEntity<PagedModel<EntityModel<Team>>> getTeamsBySponsor(
            @Parameter(description = "Id of the sponsor", example = "1") @PathVariable Long sponsorId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        if (!sponsorRepository.existsById(sponsorId))
            throw new SponsorNotFoundException(sponsorId);

        Page<Team> teamPage = repository.findBySponsorsId(sponsorId, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(teamPage, assembler));
    }

    @Operation(summary = "Constructors' championship standings",
            description = "Consulta personalizada: tabela do campeonato de construtores, somando os pontos dos resultados de todos os pilotos da equipe, ordenada por pontos")
    @ApiResponse(responseCode = "200", description = "Returned the paginated standings table")
    @GetMapping("/teams/standings")
    public ResponseEntity<PagedModel<EntityModel<Standing>>> getConstructorStandings(
            @ParameterObject @PageableDefault(size = 10, page = 0) Pageable pageable) {
        // A ordenacao e fixa (pontos desc), por isso o sort do cliente e ignorado
        Page<Standing> page = repository.findConstructorStandings(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
        AtomicLong position = new AtomicLong(page.getPageable().getOffset());
        Page<Standing> ranked = page.map(standing -> standing.withPosition(position.incrementAndGet()));

        return ResponseEntity.ok(standingsAssembler.toModel(ranked, standing -> EntityModel.of(standing,
                linkTo(methodOn(TeamController.class).getTeamById(standing.id())).withRel("team"))));
    }

    // Troca os sponsors recebidos (so com id) pelas entidades do banco
    private Set<Sponsor> resolveSponsors(Set<Sponsor> sponsors) {
        Set<Sponsor> resolved = new HashSet<>();
        if (sponsors == null)
            return resolved;

        for (Sponsor sponsor : sponsors)
            resolved.add(sponsorRepository.findById(sponsor.getId())
                    .orElseThrow(() -> new SponsorNotFoundException(sponsor.getId())));
        return resolved;
    }
}
