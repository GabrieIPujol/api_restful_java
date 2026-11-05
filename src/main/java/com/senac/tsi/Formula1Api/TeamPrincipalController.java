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
@Tag(name = "Chefes de equipe", description = "Chefes de equipe (Team Principals). Cada equipe tem no maximo um chefe "
        + "e cada chefe comanda no maximo uma equipe (One-to-One)")
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
    @Operation(summary = "Lista os chefes de equipe", description = "Lista paginada de todos os chefes, cada um com a equipe que comanda. "
            + "Use page, size e sort para navegar e ordenar (ex.: sort=since,desc)")
    @ApiResponse(responseCode = "200", description = "Lista paginada de chefes de equipe")
    @GetMapping("/principals")
    public ResponseEntity<PagedModel<EntityModel<TeamPrincipal>>> getAllPrincipals(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }

    // GET /principals/{id} - 404 se nao achar
    @Operation(summary = "Busca um chefe de equipe pelo id",
            description = "Retorna o chefe com a equipe que ele comanda e o link para ela (team)")
    @ApiResponse(responseCode = "200", description = "Chefe de equipe encontrado",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = TeamPrincipal.class)))
    @ApiResponse(responseCode = "404", description = "Chefe nao encontrado - nao existe chefe com esse id "
            + "(nunca foi criado ou ja foi excluido)", content = @Content)
    @ApiResponse(responseCode = "400", description = "Id invalido - o id precisa ser um numero inteiro (ex.: 1); "
            + "texto como abc e recusado", content = @Content)
    @GetMapping("/principals/{id}")
    public EntityModel<TeamPrincipal> getPrincipalById(@Parameter(description = "Id do chefe de equipe", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    // POST /principals - se a equipe ja tiver chefe o banco barra e volta 409
    @Operation(summary = "Cria um chefe de equipe", description = "Cadastra um chefe novo. A equipe vai pelo id "
            + "(ex.: \"team\": { \"id\": 5 }) e e opcional: sem ela o chefe fica sem equipe. Cada equipe so pode ter um chefe, "
            + "e nos dados iniciais as 4 equipes ja tem, entao crie uma equipe antes no POST /teams (ela vira o id 5)")
    @ApiResponse(responseCode = "201", description = "Chefe criado; o header Location traz o endereco dele")
    @ApiResponse(responseCode = "400", description = "Dados invalidos - name (3 a 100 caracteres), nationality (2 a 60) "
            + "e since (ano entre 1950 e 2100) sao obrigatorios. Tambem acontece com campo do tipo errado ou JSON mal formado; "
            + "a resposta diz qual campo falhou e por que", content = @Content)
    @ApiResponse(responseCode = "404", description = "Equipe nao encontrada - o id informado em team nao existe; "
            + "crie a equipe antes ou corrija o id", content = @Content)
    @ApiResponse(responseCode = "409", description = "Equipe ja tem chefe - escolha outra equipe ou exclua o chefe atual dela antes", content = @Content)
    @PostMapping("/principals")
    public ResponseEntity<EntityModel<TeamPrincipal>> newPrincipal(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do chefe novo",
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
    @Operation(summary = "Atualiza um chefe de equipe", description = "Substitui todos os dados do chefe pelo que for enviado. "
            + "Para trocar de equipe, mande o id da equipe nova em team (ela nao pode ter outro chefe); sem team, o chefe fica sem equipe")
    @ApiResponse(responseCode = "200", description = "Chefe de equipe atualizado",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = TeamPrincipal.class)))
    @ApiResponse(responseCode = "400", description = "Dados invalidos - o id nao e um numero inteiro, ou algum campo do JSON "
            + "e invalido (mesmas regras do POST)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Chefe ou equipe nao encontrado - nao existe chefe com esse id, "
            + "ou o id informado em team nao existe", content = @Content)
    @ApiResponse(responseCode = "409", description = "Equipe ja tem chefe - a equipe informada ja e comandada por outro chefe", content = @Content)
    @PutMapping("/principals/{id}")
    public ResponseEntity<EntityModel<TeamPrincipal>> updatePrincipal(
            @Parameter(description = "Id do chefe de equipe", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados do chefe",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TeamPrincipal.class),
                            examples = @ExampleObject(value = PRINCIPAL_EXAMPLE)))
            @RequestBody @Valid TeamPrincipal newPrincipal) {

        return ResponseEntity.ok(assembler.toModel(service.update(id, newPrincipal)));
    }

    // DELETE /principals/{id} - o vinculo com a equipe e desfeito antes
    @Operation(summary = "Exclui um chefe de equipe", description = "Apaga o chefe e desfaz o vinculo com a equipe, "
            + "que continua existindo e fica livre para receber outro chefe")
    @ApiResponse(responseCode = "204", description = "Chefe excluido", content = @Content)
    @ApiResponse(responseCode = "404", description = "Chefe nao encontrado - nao existe chefe com esse id "
            + "(talvez ja tenha sido excluido)", content = @Content)
    @DeleteMapping("/principals/{id}")
    public ResponseEntity<Void> deletePrincipal(@Parameter(description = "Id do chefe de equipe (o 5 e o criado no POST de exemplo)", example = "5") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /principals/search?nationality= - chefes de uma nacionalidade
    @Operation(summary = "Busca chefes pela nacionalidade", description = "Consulta personalizada: chefes de uma nacionalidade. "
            + "A nacionalidade precisa ser completa, mas maiusculas e minusculas nao importam (italian encontra Italian)")
    @ApiResponse(responseCode = "200", description = "Lista paginada dos chefes dessa nacionalidade (vazia se nenhum for)")
    @GetMapping("/principals/search")
    public ResponseEntity<PagedModel<EntityModel<TeamPrincipal>>> getPrincipalsByNationality(
            @Parameter(description = "Nacionalidade, completa e em ingles", example = "Italian") @RequestParam String nationality,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.searchByNationality(nationality, pageable), assembler));
    }

    // GET /principals/team/{teamId} - o chefe de uma equipe, volta um so e nao uma pagina
    @Operation(summary = "Busca o chefe de uma equipe", description = "Consulta personalizada (One-to-One): o chefe que comanda a equipe")
    @ApiResponse(responseCode = "200", description = "Chefe da equipe",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = TeamPrincipal.class)))
    @ApiResponse(responseCode = "404", description = "Equipe nao encontrada ou sem chefe - nao existe equipe com esse id, "
            + "ou ela ainda nao tem chefe", content = @Content)
    @GetMapping("/principals/team/{teamId}")
    public ResponseEntity<EntityModel<TeamPrincipal>> getPrincipalByTeam(
            @Parameter(description = "Id da equipe", example = "1") @PathVariable Long teamId) {
        return ResponseEntity.ok(assembler.toModel(service.findByTeam(teamId)));
    }
}
