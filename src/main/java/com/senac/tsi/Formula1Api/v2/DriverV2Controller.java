package com.senac.tsi.Formula1Api.v2;

import com.senac.tsi.Formula1Api.Driver;
import com.senac.tsi.Formula1Api.DriverService;
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
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

// versao 2 dos pilotos, mesmas URLs da v1, escolhida com o header "API-Version: 2" (ou ?api-version=2)
// o que mudou em relacao a v1 (tudo mudanca que quebra quem usa a v1, por isso e uma versao nova):
//   - a equipe vai como "teamId": 1 em vez de "team": { "id": 1 }
//   - a resposta traz a equipe resumida { id, name } e a idade calculada
//   - entrada e saida sao DTOs (records), nao a entidade do JPA
//   - JSON simples, sem HATEOAS (_links / _embedded)
// DELETE, busca, pilotos da equipe e classificacao nao mudaram, entao nao estao aqui: os mapeamentos sem versao
// do DriverController servem as duas versoes. a regra de negocio e a mesma da v1, as duas usam o DriverService
@RestController
@RequestMapping("/drivers")
@Tag(name = "Drivers v2", description = "Gerenciamento dos pilotos - versao 2 (mande o header API-Version: 2)")
public class DriverV2Controller {

    // exemplos do Swagger: o POST cria um piloto novo e o PUT altera o piloto 1, que ja existe
    private static final String NEW_DRIVER_EXAMPLE = """
            { "name": "Fernando Alonso", "number": 14, "nationality": "Spanish", "birthDate": "1981-07-29",
              "worldTitles": 2, "teamId": 1 }""";

    private static final String DRIVER_EXAMPLE = """
            { "name": "Lando Norris", "number": 4, "nationality": "British", "birthDate": "1999-11-13",
              "worldTitles": 1, "teamId": 1 }""";

    private final DriverService service;

    public DriverV2Controller(DriverService service) {
        this.service = service;
    }

    // GET /drivers (v2) - lista paginada, com content e page no lugar do _embedded
    @Operation(summary = "Get all drivers (v2)", description = "Lista paginada de pilotos no formato da v2")
    @ApiResponse(responseCode = "200", description = "Returned a page of drivers")
    @GetMapping(version = "2")
    public PagedModel<DriverResponse> getAllDrivers(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return new PagedModel<>(service.findAll(pageable).map(DriverResponse::from));
    }

    // GET /drivers/{id} (v2)
    @Operation(summary = "Get a driver by its id (v2)", description = "Retorna o piloto no formato da v2, com a idade e a equipe resumida")
    @ApiResponse(responseCode = "200", description = "Returns the driver")
    @ApiResponse(responseCode = "404", description = "Driver not found", content = @Content)
    @GetMapping(path = "/{id}", version = "2")
    public DriverResponse getDriverById(@Parameter(description = "Id of the driver", example = "1") @PathVariable Long id) {
        return DriverResponse.from(service.findById(id));
    }

    // POST /drivers (v2) - 201 com o header Location
    @Operation(summary = "Creates a new driver (v2)", description = "A equipe vai pelo campo teamId")
    @ApiResponse(responseCode = "201", description = "Returns the new driver")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Referenced team not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Car number already in use", content = @Content)
    @PostMapping(version = "2")
    public ResponseEntity<DriverResponse> newDriver(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New driver",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DriverRequest.class),
                            examples = @ExampleObject(value = NEW_DRIVER_EXAMPLE)))
            @RequestBody @Valid DriverRequest request) {
        Driver saved = service.create(request.toEntity());

        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(DriverResponse.from(saved));
    }

    // PUT /drivers/{id} (v2) - precisa mandar o JSON completo
    @Operation(summary = "Updates a driver (v2)", description = "Substitui todos os dados do piloto. A equipe vai pelo campo teamId")
    @ApiResponse(responseCode = "200", description = "Returns the updated driver")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Driver or team not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Car number already in use", content = @Content)
    @PutMapping(path = "/{id}", version = "2")
    public DriverResponse updateDriver(
            @Parameter(description = "Id of the driver", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated driver",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DriverRequest.class),
                            examples = @ExampleObject(value = DRIVER_EXAMPLE)))
            @RequestBody @Valid DriverRequest request) {
        return DriverResponse.from(service.update(id, request.toEntity()));
    }
}
