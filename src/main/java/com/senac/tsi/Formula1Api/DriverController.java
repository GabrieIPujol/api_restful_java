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

import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

// endpoints dos pilotos, o @Tag cria o grupo "Drivers" no Swagger
@RestController
@Tag(name = "Drivers", description = "Gerenciamento dos pilotos (Many-to-One com Team)")
public class DriverController {

    // exemplo que aparece no Swagger, usado no POST e no PUT
    private static final String DRIVER_EXAMPLE = """
            { "name": "Lando Norris", "number": 4, "nationality": "British", "birthDate": "1999-11-13",
              "worldTitles": 1, "team": { "id": 1 } }""";

    private final DriverRepository repository;
    private final TeamRepository teamRepository;
    private final DriverModelAssembler assembler;
    private final PagedResourcesAssembler<Driver> pagedResourcesAssembler;
    private final PagedResourcesAssembler<Standing> standingsAssembler;

    // o Spring entrega tudo pronto pelo construtor (injecao de dependencia)
    public DriverController(DriverRepository repository,
                            TeamRepository teamRepository,
                            DriverModelAssembler assembler,
                            PagedResourcesAssembler<Driver> pagedResourcesAssembler,
                            PagedResourcesAssembler<Standing> standingsAssembler) {
        this.repository = repository;
        this.teamRepository = teamRepository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.standingsAssembler = standingsAssembler;
    }

    // GET /drivers - lista paginada, o Spring le o ?page=&size=&sort= da URL
    @Operation(summary = "Get all drivers", description = "Lista paginada de todos os pilotos")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of drivers")
    @GetMapping("/drivers")
    public ResponseEntity<PagedModel<EntityModel<Driver>>> getAllDrivers(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Driver> driverPage = repository.findAll(pageable);
        // poe os links em cada piloto e monta o _embedded, _links e page
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(driverPage, assembler));
    }

    // GET /drivers/{id} - se nao achar lanca a exception e vira 404
    @Operation(summary = "Get a driver by its id")
    @ApiResponse(responseCode = "200", description = "Returns a valid driver",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Driver.class)))
    @ApiResponse(responseCode = "404", description = "Driver not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping("/drivers/{id}")
    public EntityModel<Driver> getDriverById(@Parameter(description = "Id of the driver", example = "1") @PathVariable Long id) {
        Driver driver = repository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException(id));
        return assembler.toModel(driver);
    }

    // POST /drivers - o @RequestBody grandao e so doc do Swagger, o pequeno e que le o JSON
    // o @Valid roda as validacoes antes de entrar no metodo (400 se falhar)
    @Operation(summary = "Creates a new driver", description = "A equipe e vinculada pelo id")
    @ApiResponse(responseCode = "201", description = "Driver created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Referenced team not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Car number already in use", content = @Content)
    @PostMapping("/drivers")
    public ResponseEntity<EntityModel<Driver>> newDriver(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New driver",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Driver.class),
                            examples = @ExampleObject(value = DRIVER_EXAMPLE)))
            @RequestBody @Valid Driver newDriver) {

        // troca a equipe que veio so com id pela equipe de verdade do banco
        newDriver.setTeam(resolveTeam(newDriver.getTeam()));
        EntityModel<Driver> entityModel = assembler.toModel(repository.save(newDriver));

        // 201 com o header Location apontando pro piloto novo
        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    // PUT /drivers/{id} - precisa mandar o JSON completo
    @Operation(summary = "Updates a driver")
    @ApiResponse(responseCode = "200", description = "Driver updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Driver.class)))
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Driver or team not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Car number already in use", content = @Content)
    @PutMapping("/drivers/{id}")
    public ResponseEntity<EntityModel<Driver>> updateDriver(
            @Parameter(description = "Id of the driver", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated driver",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Driver.class),
                            examples = @ExampleObject(value = DRIVER_EXAMPLE)))
            @RequestBody @Valid Driver newDriver) {

        // achou: copia os campos e salva (como ja tem id vira UPDATE), nao achou: 404
        Driver updated = repository.findById(id)
                .map(driver -> {
                    driver.setName(newDriver.getName());
                    driver.setNumber(newDriver.getNumber());
                    driver.setNationality(newDriver.getNationality());
                    driver.setBirthDate(newDriver.getBirthDate());
                    driver.setWorldTitles(newDriver.getWorldTitles());
                    driver.setTeam(resolveTeam(newDriver.getTeam()));
                    return repository.save(driver);
                })
                .orElseThrow(() -> new DriverNotFoundException(id));

        return ResponseEntity.ok(assembler.toModel(updated));
    }

    // DELETE /drivers/{id}
    @Operation(summary = "Deletes a driver", description = "So e possivel excluir pilotos sem resultados registrados")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a driver", content = @Content)
    @ApiResponse(responseCode = "404", description = "Driver not found, maybe it's already deleted", content = @Content)
    @ApiResponse(responseCode = "409", description = "Driver still has race results", content = @Content)
    @DeleteMapping("/drivers/{id}")
    public ResponseEntity<?> deleteDriver(@Parameter(description = "Id of the driver", example = "1") @PathVariable Long id) {
        var driver = repository.findById(id);
        if (driver.isEmpty())
            return ResponseEntity.notFound().build();

        // piloto com resultado nao pode sair, senao os resultados ficam orfaos
        if (!driver.get().getResults().isEmpty())
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Driver still has race results");

        repository.delete(driver.get());
        return ResponseEntity.noContent().build();
    }

    // GET /drivers/search?name= - busca por parte do nome
    @Operation(summary = "Search drivers by name", description = "Consulta personalizada: pilotos cujo nome contem o texto (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of drivers")
    @GetMapping("/drivers/search")
    public ResponseEntity<PagedModel<EntityModel<Driver>>> getDriversByName(
            @Parameter(description = "Part of the driver's name", example = "lando") @RequestParam String name,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Driver> driverPage = repository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(driverPage, assembler));
    }

    // GET /drivers/team/{teamId} - pilotos de uma equipe
    @Operation(summary = "Get drivers of a team", description = "Consulta personalizada: pilotos de uma equipe (One-to-Many)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of drivers of the team")
    @ApiResponse(responseCode = "404", description = "Team not found", content = @Content)
    @GetMapping("/drivers/team/{teamId}")
    public ResponseEntity<PagedModel<EntityModel<Driver>>> getDriversByTeam(
            @Parameter(description = "Id of the team", example = "1") @PathVariable Long teamId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        // sem essa checagem uma equipe que nao existe voltaria lista vazia em vez de 404
        if (!teamRepository.existsById(teamId))
            throw new TeamNotFoundException(teamId);

        Page<Driver> driverPage = repository.findByTeamId(teamId, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(driverPage, assembler));
    }

    // GET /drivers/standings - tabela do campeonato de pilotos
    @Operation(summary = "Drivers' championship standings",
            description = "Consulta personalizada: tabela do campeonato de pilotos, somando os pontos de todos os resultados, ordenada por pontos")
    @ApiResponse(responseCode = "200", description = "Returned the paginated standings table")
    @GetMapping("/drivers/standings")
    public ResponseEntity<PagedModel<EntityModel<Standing>>> getDriverStandings(
            @ParameterObject @PageableDefault(size = 10, page = 0) Pageable pageable) {
        // ignora o sort de quem chamou, a tabela sempre ordena por pontos
        Page<Standing> page = repository.findDriverStandings(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
        // a posicao comeca de onde a pagina anterior parou e vai somando 1
        AtomicLong position = new AtomicLong(page.getPageable().getOffset());
        Page<Standing> ranked = page.map(standing -> standing.withPosition(position.incrementAndGet()));

        // cada linha da tabela ganha um link pro piloto
        return ResponseEntity.ok(standingsAssembler.toModel(ranked, standing -> EntityModel.of(standing,
                linkTo(methodOn(DriverController.class).getDriverById(standing.id())).withRel("driver"))));
    }

    // pega a equipe que veio so com id e busca a completa no banco (404 se nao existir)
    private Team resolveTeam(Team team) {
        return teamRepository.findById(team.getId())
                .orElseThrow(() -> new TeamNotFoundException(team.getId()));
    }
}
