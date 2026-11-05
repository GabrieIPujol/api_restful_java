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
@Tag(name = "Equipes", description = "Equipes (construtores) de Formula 1. Cada equipe tem varios pilotos (One-to-Many), "
        + "um chefe (One-to-One) e varios patrocinadores (Many-to-Many)")
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
    @Operation(summary = "Lista as equipes", description = "Lista paginada de todas as equipes, cada uma com seus patrocinadores. "
            + "Use page, size e sort para navegar e ordenar (ex.: sort=name,asc)")
    @ApiResponse(responseCode = "200", description = "Lista paginada de equipes")
    @GetMapping("/teams")
    public ResponseEntity<PagedModel<EntityModel<Team>>> getAllTeams(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }

    // GET /teams/{id} - 404 se nao achar
    @Operation(summary = "Busca uma equipe pelo id",
            description = "Retorna a equipe com seus patrocinadores e os links para os pilotos (drivers) e o chefe (principal)")
    @ApiResponse(responseCode = "200", description = "Equipe encontrada",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Team.class)))
    @ApiResponse(responseCode = "404", description = "Equipe nao encontrada - nao existe equipe com esse id "
            + "(nunca foi criada ou ja foi excluida)", content = @Content)
    @ApiResponse(responseCode = "400", description = "Id invalido - o id precisa ser um numero inteiro (ex.: 1); "
            + "texto como abc e recusado", content = @Content)
    @GetMapping("/teams/{id}")
    public EntityModel<Team> getTeamById(@Parameter(description = "Id da equipe", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    // POST /teams - cria a equipe ja com os patrocinadores
    @Operation(summary = "Cria uma equipe", description = "Cadastra uma equipe nova. Os patrocinadores sao ligados pelo id, "
            + "numa lista (ex.: \"sponsors\": [ { \"id\": 2 } ]), e precisam existir antes; a lista pode ir vazia. "
            + "O nome da equipe nao pode repetir")
    @ApiResponse(responseCode = "201", description = "Equipe criada; o header Location traz o endereco dela")
    @ApiResponse(responseCode = "400", description = "Dados invalidos - name e country sao obrigatorios (2 a 100 e 2 a 60 caracteres), "
            + "base tem ate 100, foundedYear vai de 1900 a 2100 e constructorTitles nao pode ser negativo. Tambem acontece "
            + "com campo do tipo errado ou JSON mal formado; a resposta diz qual campo falhou e por que", content = @Content)
    @ApiResponse(responseCode = "404", description = "Patrocinador nao encontrado - algum id da lista sponsors nao existe; "
            + "crie o patrocinador antes ou corrija o id", content = @Content)
    @ApiResponse(responseCode = "409", description = "Nome repetido - ja existe uma equipe com esse nome", content = @Content)
    @PostMapping("/teams")
    public ResponseEntity<EntityModel<Team>> newTeam(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados da equipe nova",
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
    @Operation(summary = "Atualiza uma equipe", description = "Substitui todos os dados da equipe pelo que for enviado, "
            + "inclusive a lista de patrocinadores: quem ficar fora da lista deixa de patrocinar a equipe. "
            + "Os pilotos e o chefe nao mudam")
    @ApiResponse(responseCode = "200", description = "Equipe atualizada",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Team.class)))
    @ApiResponse(responseCode = "400", description = "Dados invalidos - o id nao e um numero inteiro, ou algum campo do JSON "
            + "e invalido (mesmas regras do POST)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Equipe ou patrocinador nao encontrado - nao existe equipe com esse id, "
            + "ou algum id da lista sponsors nao existe", content = @Content)
    @ApiResponse(responseCode = "409", description = "Nome repetido - outra equipe ja usa esse nome", content = @Content)
    @PutMapping("/teams/{id}")
    public ResponseEntity<EntityModel<Team>> updateTeam(
            @Parameter(description = "Id da equipe", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados da equipe",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Team.class),
                            examples = @ExampleObject(value = TEAM_EXAMPLE)))
            @RequestBody @Valid Team newTeam) {

        return ResponseEntity.ok(assembler.toModel(service.update(id, newTeam)));
    }

    // DELETE /teams/{id}
    @Operation(summary = "Exclui uma equipe", description = "Apaga a equipe e os vinculos dela com os patrocinadores "
            + "(os patrocinadores continuam existindo). So da para excluir equipe sem pilotos e sem chefe: "
            + "transfira ou exclua os pilotos e exclua o chefe antes")
    @ApiResponse(responseCode = "204", description = "Equipe excluida", content = @Content)
    @ApiResponse(responseCode = "404", description = "Equipe nao encontrada - nao existe equipe com esse id "
            + "(talvez ja tenha sido excluida)", content = @Content)
    @ApiResponse(responseCode = "409", description = "Equipe em uso - ela ainda tem pilotos ou um chefe vinculados", content = @Content)
    @DeleteMapping("/teams/{id}")
    public ResponseEntity<Void> deleteTeam(@Parameter(description = "Id da equipe (o 5 e a criada no POST de exemplo)", example = "5") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /teams/search?country= - equipes de um pais
    @Operation(summary = "Busca equipes pelo pais", description = "Consulta personalizada: equipes de um pais. "
            + "O nome do pais precisa ser completo, mas maiusculas e minusculas nao importam (italy encontra Italy)")
    @ApiResponse(responseCode = "200", description = "Lista paginada das equipes do pais (vazia se nenhuma for de la)")
    @GetMapping("/teams/search")
    public ResponseEntity<PagedModel<EntityModel<Team>>> getTeamsByCountry(
            @Parameter(description = "Pais da equipe, completo e em ingles", example = "Italy") @RequestParam String country,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.searchByCountry(country, pageable), assembler));
    }

    // GET /teams/sponsor/{sponsorId} - equipes de um patrocinador
    @Operation(summary = "Lista as equipes de um patrocinador",
            description = "Consulta personalizada (Many-to-Many): equipes que tem esse patrocinador na lista sponsors")
    @ApiResponse(responseCode = "200", description = "Lista paginada das equipes do patrocinador")
    @ApiResponse(responseCode = "404", description = "Patrocinador nao encontrado - nao existe patrocinador com esse id", content = @Content)
    @GetMapping("/teams/sponsor/{sponsorId}")
    public ResponseEntity<PagedModel<EntityModel<Team>>> getTeamsBySponsor(
            @Parameter(description = "Id do patrocinador", example = "1") @PathVariable Long sponsorId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findBySponsor(sponsorId, pageable), assembler));
    }

    // GET /teams/standings - tabela de construtores
    @Operation(summary = "Classificacao de construtores", description = "Consulta personalizada: tabela do campeonato de construtores. "
            + "Soma os pontos de todos os resultados dos pilotos de cada equipe e ordena do maior para o menor; "
            + "cada linha traz a posicao e um link para a equipe (team). A ordem e sempre por pontos, por isso a rota nao tem sort")
    @ApiResponse(responseCode = "200", description = "Tabela de classificacao paginada")
    // a tabela sempre ordena por pontos, entao no swagger so aparecem page e size (o sort seria ignorado)
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Numero da pagina, comecando em 0",
            schema = @Schema(type = "integer", defaultValue = "0"))
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "Quantos registros por pagina",
            schema = @Schema(type = "integer", defaultValue = "10"))
    @GetMapping("/teams/standings")
    public ResponseEntity<PagedModel<EntityModel<Standing>>> getConstructorStandings(
            @Parameter(hidden = true) @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ResponseEntity.ok(standingsAssembler.toModel(service.standings(pageable), standing -> EntityModel.of(standing,
                linkTo(methodOn(TeamController.class).getTeamById(standing.id())).withRel("team"))));
    }
}
