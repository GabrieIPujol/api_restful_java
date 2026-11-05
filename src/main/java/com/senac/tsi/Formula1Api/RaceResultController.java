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
@Tag(name = "Resultados", description = "Resultados de corrida: a posicao e os pontos de um piloto em um Grande Premio. "
        + "Sao a base das classificacoes, entao criar, alterar ou excluir um resultado muda as tabelas na hora")
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
    @Operation(summary = "Lista os resultados", description = "Lista paginada de todos os resultados, cada um com o piloto. "
            + "Use page, size e sort para navegar e ordenar (ex.: sort=raceDate,desc)")
    @ApiResponse(responseCode = "200", description = "Lista paginada de resultados")
    @GetMapping("/results")
    public ResponseEntity<PagedModel<EntityModel<RaceResult>>> getAllResults(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }

    // GET /results/{id} - 404 se nao achar
    @Operation(summary = "Busca um resultado pelo id",
            description = "Retorna o resultado com o piloto e o link para ele (driver)")
    @ApiResponse(responseCode = "200", description = "Resultado encontrado",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = RaceResult.class)))
    @ApiResponse(responseCode = "404", description = "Resultado nao encontrado - nao existe resultado com esse id "
            + "(nunca foi criado ou ja foi excluido)", content = @Content)
    @ApiResponse(responseCode = "400", description = "Id invalido - o id precisa ser um numero inteiro (ex.: 1); "
            + "texto como abc e recusado", content = @Content)
    @GetMapping("/results/{id}")
    public EntityModel<RaceResult> getResultById(@Parameter(description = "Id do resultado", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    // POST /results - registra uma corrida e os pontos ja entram na tabela
    @Operation(summary = "Cria um resultado", description = "Registra o resultado de um piloto em uma corrida. O piloto vai pelo id "
            + "(ex.: \"driver\": { \"id\": 1 }) e precisa existir antes. Os pontos ja entram na classificacao de pilotos "
            + "e na de construtores (pela equipe do piloto)")
    @ApiResponse(responseCode = "201", description = "Resultado criado; o header Location traz o endereco dele")
    @ApiResponse(responseCode = "400", description = "Dados invalidos - raceName e circuit (3 a 100 caracteres), "
            + "raceDate (formato AAAA-MM-DD), position (1 a 30), points (0 a 26), status e driver sao obrigatorios; "
            + "status so aceita FINISHED (terminou), DNF (abandonou) ou DSQ (desclassificado). Tambem acontece com campo "
            + "do tipo errado ou JSON mal formado; a resposta diz qual campo falhou e por que", content = @Content)
    @ApiResponse(responseCode = "404", description = "Piloto nao encontrado - o id informado em driver nao existe; "
            + "crie o piloto antes ou corrija o id", content = @Content)
    @PostMapping("/results")
    public ResponseEntity<EntityModel<RaceResult>> newResult(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do resultado novo",
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
    @Operation(summary = "Atualiza um resultado", description = "Substitui todos os dados do resultado pelo que for enviado. "
            + "Se os pontos ou o piloto mudarem, as classificacoes ja mostram a diferenca")
    @ApiResponse(responseCode = "200", description = "Resultado atualizado",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = RaceResult.class)))
    @ApiResponse(responseCode = "400", description = "Dados invalidos - o id nao e um numero inteiro, ou algum campo do JSON "
            + "e invalido (mesmas regras do POST)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Resultado ou piloto nao encontrado - nao existe resultado com esse id, "
            + "ou o id informado em driver nao existe", content = @Content)
    @PutMapping("/results/{id}")
    public ResponseEntity<EntityModel<RaceResult>> updateResult(
            @Parameter(description = "Id do resultado", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados do resultado",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RaceResult.class),
                            examples = @ExampleObject(value = RESULT_EXAMPLE)))
            @RequestBody @Valid RaceResult newResult) {

        return ResponseEntity.ok(assembler.toModel(service.update(id, newResult)));
    }

    // DELETE /results/{id}
    @Operation(summary = "Exclui um resultado", description = "Apaga o resultado, e os pontos dele saem das classificacoes na hora. "
            + "E o passo necessario antes de excluir um piloto")
    @ApiResponse(responseCode = "204", description = "Resultado excluido", content = @Content)
    @ApiResponse(responseCode = "404", description = "Resultado nao encontrado - nao existe resultado com esse id "
            + "(talvez ja tenha sido excluido)", content = @Content)
    @DeleteMapping("/results/{id}")
    public ResponseEntity<Void> deleteResult(@Parameter(description = "Id do resultado (o 17 e o criado no POST de exemplo)", example = "17") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /results/status/{status} - filtra pelo enum, valor invalido volta 400
    @Operation(summary = "Lista os resultados por status", description = "Consulta personalizada: resultados com um status do enum "
            + "ResultStatus - FINISHED (terminou), DNF (abandonou) ou DSQ (desclassificado). Ex.: DNF lista todos os abandonos")
    @ApiResponse(responseCode = "200", description = "Lista paginada dos resultados com esse status")
    @ApiResponse(responseCode = "400", description = "Status invalido - use FINISHED, DNF ou DSQ, em maiusculas", content = @Content)
    @GetMapping("/results/status/{status}")
    public ResponseEntity<PagedModel<EntityModel<RaceResult>>> getResultsByStatus(
            @Parameter(description = "Status do resultado: FINISHED, DNF ou DSQ", example = "DNF") @PathVariable ResultStatus status,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByStatus(status, pageable), assembler));
    }

    // GET /results/search?race= - resultados de um GP, ordenados pela posicao
    @Operation(summary = "Busca resultados pela corrida", description = "Consulta personalizada: resultados das corridas cujo nome "
            + "contem o texto, em qualquer parte e sem diferenciar maiusculas (australian encontra Australian Grand Prix). "
            + "Sem sort, vem ordenado pela posicao, como o grid de chegada")
    @ApiResponse(responseCode = "200", description = "Lista paginada dos resultados da corrida (vazia se nenhum nome bater)")
    @GetMapping("/results/search")
    public ResponseEntity<PagedModel<EntityModel<RaceResult>>> getResultsByRace(
            @Parameter(description = "Parte do nome da corrida", example = "australian") @RequestParam String race,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "position") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.searchByRace(race, pageable), assembler));
    }

    // GET /results/driver/{driverId} - historico do piloto, ordenado pela data
    @Operation(summary = "Lista os resultados de um piloto", description = "Consulta personalizada (One-to-Many): historico de corridas "
            + "do piloto. Sem sort, vem em ordem de data")
    @ApiResponse(responseCode = "200", description = "Lista paginada dos resultados do piloto")
    @ApiResponse(responseCode = "404", description = "Piloto nao encontrado - nao existe piloto com esse id", content = @Content)
    @GetMapping("/results/driver/{driverId}")
    public ResponseEntity<PagedModel<EntityModel<RaceResult>>> getResultsByDriver(
            @Parameter(description = "Id do piloto", example = "1") @PathVariable Long driverId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "raceDate") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByDriver(driverId, pageable), assembler));
    }
}
