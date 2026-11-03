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

// endpoints dos pilotos, versao 1 (a padrao quando nao manda o header API-Version)
// listar, buscar, criar e alterar tem uma v2 no v2.DriverV2Controller, o resto serve as duas versoes
@RestController
@Tag(name = "Drivers", description = "Gerenciamento dos pilotos (Many-to-One com Team) - rotas da v1 e as que valem nas duas versoes")
public class DriverController {

    // exemplos do Swagger: o POST cria um registro novo e o PUT altera o id 1, que ja existe
    // (com o mesmo exemplo nos dois, o PUT dava 409 de valor repetido depois do POST)
    private static final String NEW_DRIVER_EXAMPLE = """
            { "name": "Carlos Sainz", "number": 55, "nationality": "Spanish", "birthDate": "1994-09-01",
              "worldTitles": 0, "team": { "id": 2 } }""";

    private static final String DRIVER_EXAMPLE = """
            { "name": "Lando Norris", "number": 4, "nationality": "British", "birthDate": "1999-11-13",
              "worldTitles": 1, "team": { "id": 1 } }""";

    private final DriverService service;
    private final DriverModelAssembler assembler;
    private final PagedResourcesAssembler<Driver> pagedResourcesAssembler;
    private final PagedResourcesAssembler<Standing> standingsAssembler;

    // o Spring entrega tudo pronto pelo construtor (injecao de dependencia)
    public DriverController(DriverService service,
                            DriverModelAssembler assembler,
                            PagedResourcesAssembler<Driver> pagedResourcesAssembler,
                            PagedResourcesAssembler<Standing> standingsAssembler) {
        this.service = service;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.standingsAssembler = standingsAssembler;
    }

    // GET /drivers - lista paginada, o Spring le o ?page=&size=&sort= da URL
    @Operation(summary = "Get all drivers", description = "Lista paginada de todos os pilotos")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of drivers")
    @GetMapping(path = "/drivers", version = "1")
    public ResponseEntity<PagedModel<EntityModel<Driver>>> getAllDrivers(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        // poe os links em cada piloto e monta o _embedded, _links e page
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }

    // GET /drivers/{id} - se nao achar o service lanca a exception e vira 404
    @Operation(summary = "Get a driver by its id", description = "Retorna um piloto com a equipe e os links HATEOAS")
    @ApiResponse(responseCode = "200", description = "Returns a valid driver",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Driver.class)))
    @ApiResponse(responseCode = "404", description = "Driver not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping(path = "/drivers/{id}", version = "1")
    public EntityModel<Driver> getDriverById(@Parameter(description = "Id of the driver", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    // POST /drivers - o @RequestBody grandao e so doc do Swagger, o pequeno e que le o JSON
    // o @Valid roda as validacoes antes de entrar no metodo (400 se falhar)
    @Operation(summary = "Creates a new driver", description = "A equipe e vinculada pelo id")
    @ApiResponse(responseCode = "201", description = "Driver created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Referenced team not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Car number already in use", content = @Content)
    @PostMapping(path = "/drivers", version = "1")
    public ResponseEntity<EntityModel<Driver>> newDriver(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New driver",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Driver.class),
                            examples = @ExampleObject(value = NEW_DRIVER_EXAMPLE)))
            @RequestBody @Valid Driver newDriver) {

        EntityModel<Driver> entityModel = assembler.toModel(service.create(newDriver));

        // 201 com o header Location apontando pro piloto novo
        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    // PUT /drivers/{id} - precisa mandar o JSON completo
    @Operation(summary = "Updates a driver", description = "Substitui todos os dados do piloto. A equipe e vinculada pelo id")
    @ApiResponse(responseCode = "200", description = "Driver updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Driver.class)))
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Driver or team not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Car number already in use", content = @Content)
    @PutMapping(path = "/drivers/{id}", version = "1")
    public ResponseEntity<EntityModel<Driver>> updateDriver(
            @Parameter(description = "Id of the driver", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated driver",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Driver.class),
                            examples = @ExampleObject(value = DRIVER_EXAMPLE)))
            @RequestBody @Valid Driver newDriver) {

        return ResponseEntity.ok(assembler.toModel(service.update(id, newDriver)));
    }

    // DELETE /drivers/{id} - sem versao, e igual na v1 e na v2
    @Operation(summary = "Deletes a driver", description = "So e possivel excluir pilotos sem resultados registrados")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a driver", content = @Content)
    @ApiResponse(responseCode = "404", description = "Driver not found, maybe it's already deleted", content = @Content)
    @ApiResponse(responseCode = "409", description = "Driver still has race results", content = @Content)
    @DeleteMapping("/drivers/{id}")
    public ResponseEntity<Void> deleteDriver(@Parameter(description = "Id of the driver (o 9 e o criado no POST de exemplo)", example = "9") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /drivers/search?name= - busca por parte do nome
    @Operation(summary = "Search drivers by name", description = "Consulta personalizada: pilotos cujo nome contem o texto (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of drivers")
    @GetMapping("/drivers/search")
    public ResponseEntity<PagedModel<EntityModel<Driver>>> getDriversByName(
            @Parameter(description = "Part of the driver's name", example = "lando") @RequestParam String name,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.searchByName(name, pageable), assembler));
    }

    // GET /drivers/team/{teamId} - pilotos de uma equipe
    @Operation(summary = "Get drivers of a team", description = "Consulta personalizada: pilotos de uma equipe (One-to-Many)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of drivers of the team")
    @ApiResponse(responseCode = "404", description = "Team not found", content = @Content)
    @GetMapping("/drivers/team/{teamId}")
    public ResponseEntity<PagedModel<EntityModel<Driver>>> getDriversByTeam(
            @Parameter(description = "Id of the team", example = "1") @PathVariable Long teamId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByTeam(teamId, pageable), assembler));
    }

    // GET /drivers/standings - tabela do campeonato de pilotos
    @Operation(summary = "Drivers' championship standings",
            description = "Consulta personalizada: tabela do campeonato de pilotos, somando os pontos de todos os resultados, ordenada por pontos")
    @ApiResponse(responseCode = "200", description = "Returned the paginated standings table")
    // a tabela sempre ordena por pontos, entao no swagger so aparecem page e size (o sort seria ignorado)
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Zero-based page index (0..N)",
            schema = @Schema(type = "integer", defaultValue = "0"))
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "The size of the page to be returned",
            schema = @Schema(type = "integer", defaultValue = "10"))
    @GetMapping("/drivers/standings")
    public ResponseEntity<PagedModel<EntityModel<Standing>>> getDriverStandings(
            @Parameter(hidden = true) @PageableDefault(size = 10, page = 0) Pageable pageable) {
        // cada linha da tabela ganha um link pro piloto
        return ResponseEntity.ok(standingsAssembler.toModel(service.standings(pageable), standing -> EntityModel.of(standing,
                linkTo(methodOn(DriverController.class).getDriverById(standing.id())).withRel("driver"))));
    }
}
