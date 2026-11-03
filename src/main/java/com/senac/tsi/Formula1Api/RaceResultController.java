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

// endpoints dos resultados, mesmo molde do DriverController
@RestController
@Tag(name = "Race Results", description = "Gerenciamento dos resultados de corrida, base para a pontuacao e as tabelas de classificacao")
public class RaceResultController {

    // exemplos do Swagger: o POST cria um registro novo e o PUT altera o id 1, que ja existe
    // (com o mesmo exemplo nos dois, o PUT dava 409 de valor repetido depois do POST)
    private static final String NEW_RESULT_EXAMPLE = """
            { "raceName": "Japanese Grand Prix", "circuit": "Suzuka", "raceDate": "2026-03-29",
              "position": 1, "points": 25, "status": "FINISHED", "driver": { "id": 1 } }""";

    private static final String RESULT_EXAMPLE = """
            { "raceName": "Australian Grand Prix", "circuit": "Albert Park", "raceDate": "2026-03-08",
              "position": 1, "points": 25, "status": "FINISHED", "driver": { "id": 1 } }""";

    private final RaceResultService service;
    private final RaceResultModelAssembler assembler;
    private final PagedResourcesAssembler<RaceResult> pagedResourcesAssembler;

    // injecao de dependencia pelo construtor
    public RaceResultController(RaceResultService service,
                                RaceResultModelAssembler assembler,
                                PagedResourcesAssembler<RaceResult> pagedResourcesAssembler) {
        this.service = service;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    // GET /results - lista paginada
    @Operation(summary = "Get all race results", description = "Lista paginada de todos os resultados")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of race results")
    @GetMapping("/results")
    public ResponseEntity<PagedModel<EntityModel<RaceResult>>> getAllResults(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }

    // GET /results/{id} - 404 se nao achar
    @Operation(summary = "Get a race result by its id", description = "Retorna um resultado com o piloto e os links HATEOAS")
    @ApiResponse(responseCode = "200", description = "Returns a valid race result",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = RaceResult.class)))
    @ApiResponse(responseCode = "404", description = "Race result not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping("/results/{id}")
    public EntityModel<RaceResult> getResultById(@Parameter(description = "Id of the race result", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
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
                            examples = @ExampleObject(value = NEW_RESULT_EXAMPLE)))
            @RequestBody @Valid RaceResult newResult) {

        EntityModel<RaceResult> entityModel = assembler.toModel(service.create(newResult));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    // PUT /results/{id} - da pra usar pra corrigir os pontos de uma corrida
    @Operation(summary = "Updates a race result", description = "Substitui todos os dados do resultado; a mudanca nos pontos ja aparece nas tabelas de classificacao")
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

        return ResponseEntity.ok(assembler.toModel(service.update(id, newResult)));
    }

    // DELETE /results/{id}
    @Operation(summary = "Deletes a race result", description = "Remove o resultado e os pontos dele saem das tabelas de classificacao")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a race result", content = @Content)
    @ApiResponse(responseCode = "404", description = "Race result not found, maybe it's already deleted", content = @Content)
    @DeleteMapping("/results/{id}")
    public ResponseEntity<Void> deleteResult(@Parameter(description = "Id of the race result (o 17 e o criado no POST de exemplo)", example = "17") @PathVariable Long id) {
        service.delete(id);
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
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByStatus(status, pageable), assembler));
    }

    // GET /results/search?race= - resultados de um GP, ordenados pela posicao
    @Operation(summary = "Search race results by race name", description = "Consulta personalizada: resultados de um Grande Premio, ordenados pela posicao")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of race results")
    @GetMapping("/results/search")
    public ResponseEntity<PagedModel<EntityModel<RaceResult>>> getResultsByRace(
            @Parameter(description = "Part of the race name", example = "australian") @RequestParam String race,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "position") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.searchByRace(race, pageable), assembler));
    }

    // GET /results/driver/{driverId} - historico do piloto, ordenado pela data
    @Operation(summary = "Get race results of a driver", description = "Consulta personalizada: historico de resultados de um piloto")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of race results of the driver")
    @ApiResponse(responseCode = "404", description = "Driver not found", content = @Content)
    @GetMapping("/results/driver/{driverId}")
    public ResponseEntity<PagedModel<EntityModel<RaceResult>>> getResultsByDriver(
            @Parameter(description = "Id of the driver", example = "1") @PathVariable Long driverId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "raceDate") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByDriver(driverId, pageable), assembler));
    }
}
