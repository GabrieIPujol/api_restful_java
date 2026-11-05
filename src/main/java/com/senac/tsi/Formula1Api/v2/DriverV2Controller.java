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
@Tag(name = "Pilotos v2", description = "Pilotos na versao 2, pedida com o header API-Version: 2. Muda o formato: "
        + "a equipe vai pelo campo teamId e volta resumida (so id e nome), a idade vem calculada e a resposta nao tem _links. "
        + "Excluir, buscar pelo nome, pilotos de uma equipe e classificacao sao iguais nas duas versoes e ficam no grupo Pilotos")
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
    @Operation(summary = "Lista os pilotos (v2)", description = "Lista paginada de todos os pilotos no formato da v2: "
            + "equipe resumida e idade calculada, sem _links")
    @ApiResponse(responseCode = "200", description = "Pagina de pilotos no formato da v2")
    @GetMapping(version = "2")
    public PagedModel<DriverResponse> getAllDrivers(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return new PagedModel<>(service.findAll(pageable).map(DriverResponse::from));
    }

    // GET /drivers/{id} (v2)
    @Operation(summary = "Busca um piloto pelo id (v2)", description = "Retorna o piloto no formato da v2: "
            + "a idade calculada a partir de birthDate e a equipe resumida (id e nome)")
    @ApiResponse(responseCode = "200", description = "Piloto encontrado")
    @ApiResponse(responseCode = "404", description = "Piloto nao encontrado - nao existe piloto com esse id "
            + "(nunca foi criado ou ja foi excluido)", content = @Content)
    @GetMapping(path = "/{id}", version = "2")
    public DriverResponse getDriverById(@Parameter(description = "Id do piloto", example = "1") @PathVariable Long id) {
        return DriverResponse.from(service.findById(id));
    }

    // POST /drivers (v2) - 201 com o header Location
    @Operation(summary = "Cria um piloto (v2)", description = "Cadastra um piloto novo. Na v2 a equipe vai pelo campo teamId "
            + "(ex.: \"teamId\": 1) em vez do objeto team, e precisa existir antes. O numero do carro nao pode repetir")
    @ApiResponse(responseCode = "201", description = "Piloto criado; o header Location traz o endereco dele")
    @ApiResponse(responseCode = "400", description = "Dados invalidos - name (3 a 100 caracteres), number (1 a 99), "
            + "nationality (2 a 60), birthDate (data no passado, AAAA-MM-DD), worldTitles (a partir de 0) e teamId "
            + "sao obrigatorios. Tambem acontece com campo do tipo errado ou JSON mal formado; "
            + "a resposta diz qual campo falhou e por que", content = @Content)
    @ApiResponse(responseCode = "404", description = "Equipe nao encontrada - o teamId informado nao existe; "
            + "crie a equipe antes ou corrija o id", content = @Content)
    @ApiResponse(responseCode = "409", description = "Numero repetido - outro piloto ja usa esse numero de carro", content = @Content)
    @PostMapping(version = "2")
    public ResponseEntity<DriverResponse> newDriver(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do piloto novo",
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
    @Operation(summary = "Atualiza um piloto (v2)", description = "Substitui todos os dados do piloto pelo que for enviado. "
            + "Para trocar de equipe, mande o id da equipe nova em teamId")
    @ApiResponse(responseCode = "200", description = "Piloto atualizado")
    @ApiResponse(responseCode = "400", description = "Dados invalidos - o id nao e um numero inteiro, ou algum campo do JSON "
            + "e invalido (mesmas regras do POST)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Piloto ou equipe nao encontrado - nao existe piloto com esse id, "
            + "ou o teamId informado nao existe", content = @Content)
    @ApiResponse(responseCode = "409", description = "Numero repetido - outro piloto ja usa esse numero de carro", content = @Content)
    @PutMapping(path = "/{id}", version = "2")
    public DriverResponse updateDriver(
            @Parameter(description = "Id do piloto", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados do piloto",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DriverRequest.class),
                            examples = @ExampleObject(value = DRIVER_EXAMPLE)))
            @RequestBody @Valid DriverRequest request) {
        return DriverResponse.from(service.update(id, request.toEntity()));
    }
}
