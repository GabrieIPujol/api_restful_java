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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// endpoints dos resultados, mesmo molde do DriverController
// qualquer mudanca aqui ja muda as tabelas de classificacao
@RestController
@Tag(name = "Race Results", description = "Gerenciamento dos resultados de corrida, base para a pontuacao e as tabelas de classificacao")
public class RaceResultController {

    // exemplo que aparece no Swagger
    private static final String RESULT_EXAMPLE = """
            { "raceName": "Australian Grand Prix", "circuit": "Albert Park", "raceDate": "2026-03-08",
              "position": 1, "points": 25, "status": "FINISHED", "driver": { "id": 1 } }""";

    private final RaceResultRepository repository;
    private final DriverRepository driverRepository;
    private final RaceResultModelAssembler assembler;
    private final PagedResourcesAssembler<RaceResult> pagedResourcesAssembler;

    // injecao de dependencia pelo construtor
    public RaceResultController(RaceResultRepository repository,
                                DriverRepository driverRepository,
                                RaceResultModelAssembler assembler,
                                PagedResourcesAssembler<RaceResult> pagedResourcesAssembler) {
        this.repository = repository;
        this.driverRepository = driverRepository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    // GET /results - lista paginada
    @Operation(summary = "Get all race results", description = "Lista paginada de todos os resultados")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of race results")
    @GetMapping("/results")
    public ResponseEntity<PagedModel<EntityModel<RaceResult>>> getAllResults(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<RaceResult> resultPage = repository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(resultPage, assembler));
    }

    // GET /results/{id} - 404 se nao achar
    @Operation(summary = "Get a race result by its id")
    @ApiResponse(responseCode = "200", description = "Returns a valid race result",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = RaceResult.class)))
    @ApiResponse(responseCode = "404", description = "Race result not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping("/results/{id}")
    public EntityModel<RaceResult> getResultById(@Parameter(description = "Id of the race result", example = "1") @PathVariable Long id) {
        RaceResult result = repository.findById(id)
                .orElseThrow(() -> new RaceResultNotFoundException(id));
        return assembler.toModel(result);
    }

    // POST /results - registra uma corrida e os pontos ja entram na tabela
    @Operation(summary = "Creates a new race result", description = "O piloto e vinculado pelo id; os pontos entram nas tabelas de classificacao")
    @ApiResponse(responseCode = "201", description = "Race result created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload (ex.: status fora de FINISHED, DNF, DSQ)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Referenced driver not found", content = @Content)
    @PostMapping("/results")
    public ResponseEntity<EntityModel<RaceResult>> newResult(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New race result",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RaceResult.class),
                            examples = @ExampleObject(value = RESULT_EXAMPLE)))
            @RequestBody @Valid RaceResult newResult) {

        // troca o piloto que veio so com id pelo piloto de verdade
        newResult.setDriver(resolveDriver(newResult.getDriver()));
        EntityModel<RaceResult> entityModel = assembler.toModel(repository.save(newResult));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    // PUT /results/{id} - da pra usar pra corrigir os pontos de uma corrida
    @Operation(summary = "Updates a race result")
    @ApiResponse(responseCode = "200", description = "Race result updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = RaceResult.class)))
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Race result or driver not found", content = @Content)
    @PutMapping("/results/{id}")
    public ResponseEntity<EntityModel<RaceResult>> updateResult(
            @Parameter(description = "Id of the race result", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated race result",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RaceResult.class),
                            examples = @ExampleObject(value = RESULT_EXAMPLE)))
            @RequestBody @Valid RaceResult newResult) {

        RaceResult updated = repository.findById(id)
                .map(result -> {
                    result.setRaceName(newResult.getRaceName());
                    result.setCircuit(newResult.getCircuit());
                    result.setRaceDate(newResult.getRaceDate());
                    result.setPosition(newResult.getPosition());
                    result.setPoints(newResult.getPoints());
                    result.setStatus(newResult.getStatus());
                    result.setDriver(resolveDriver(newResult.getDriver()));
                    return repository.save(result);
                })
                .orElseThrow(() -> new RaceResultNotFoundException(id));

        return ResponseEntity.ok(assembler.toModel(updated));
    }

    // DELETE /results/{id} - ninguem depende de resultado, entao nao tem 409 aqui
    @Operation(summary = "Deletes a race result")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a race result", content = @Content)
    @ApiResponse(responseCode = "404", description = "Race result not found, maybe it's already deleted", content = @Content)
    @DeleteMapping("/results/{id}")
    public ResponseEntity<?> deleteResult(@Parameter(description = "Id of the race result", example = "1") @PathVariable Long id) {
        if (!repository.existsById(id))
            return ResponseEntity.notFound().build();

        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // GET /results/status/{status} - filtra pelo enum, valor invalido volta 400
    @Operation(summary = "Get race results by status", description = "Consulta personalizada: resultados filtrados pelo enum ResultStatus (ex.: todos os abandonos - DNF)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of race results")
    @ApiResponse(responseCode = "400", description = "Invalid status", content = @Content)
    @GetMapping("/results/status/{status}")
    public ResponseEntity<PagedModel<EntityModel<RaceResult>>> getResultsByStatus(
            @Parameter(description = "Status of the result", example = "DNF") @PathVariable ResultStatus status,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<RaceResult> resultPage = repository.findByStatus(status, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(resultPage, assembler));
    }

    // GET /results/search?race= - resultados de um GP, ordenados pela posicao
    @Operation(summary = "Search race results by race name", description = "Consulta personalizada: resultados de um Grande Premio, ordenados pela posicao")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of race results")
    @GetMapping("/results/search")
    public ResponseEntity<PagedModel<EntityModel<RaceResult>>> getResultsByRace(
            @Parameter(description = "Part of the race name", example = "australian") @RequestParam String race,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "position") Pageable pageable) {
        Page<RaceResult> resultPage = repository.findByRaceNameContainingIgnoreCase(race, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(resultPage, assembler));
    }

    // GET /results/driver/{driverId} - historico do piloto, ordenado pela data
    @Operation(summary = "Get race results of a driver", description = "Consulta personalizada: historico de resultados de um piloto")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of race results of the driver")
    @ApiResponse(responseCode = "404", description = "Driver not found", content = @Content)
    @GetMapping("/results/driver/{driverId}")
    public ResponseEntity<PagedModel<EntityModel<RaceResult>>> getResultsByDriver(
            @Parameter(description = "Id of the driver", example = "1") @PathVariable Long driverId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "raceDate") Pageable pageable) {
        if (!driverRepository.existsById(driverId))
            throw new DriverNotFoundException(driverId);

        Page<RaceResult> resultPage = repository.findByDriverId(driverId, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(resultPage, assembler));
    }

    // busca o piloto de verdade no banco (404 se nao existir)
    private Driver resolveDriver(Driver driver) {
        return driverRepository.findById(driver.getId())
                .orElseThrow(() -> new DriverNotFoundException(driver.getId()));
    }
}
