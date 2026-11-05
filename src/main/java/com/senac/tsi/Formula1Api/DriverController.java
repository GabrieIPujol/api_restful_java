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
@Tag(name = "Pilotos", description = "Pilotos de Formula 1. Cada piloto corre por uma equipe (Many-to-One) e tem varios resultados. "
        + "Aqui ficam as rotas da v1 e as que valem nas duas versoes (excluir, buscar pelo nome, pilotos de uma equipe e classificacao)")
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
    @Operation(summary = "Lista os pilotos", description = "Lista paginada de todos os pilotos, cada um com a equipe completa. "
            + "Use page, size e sort para navegar e ordenar (ex.: sort=number,asc)")
    @ApiResponse(responseCode = "200", description = "Lista paginada de pilotos")
    @GetMapping(path = "/drivers", version = "1")
    public ResponseEntity<PagedModel<EntityModel<Driver>>> getAllDrivers(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        // poe os links em cada piloto e monta o _embedded, _links e page
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }

    // GET /drivers/{id} - se nao achar o service lanca a exception e vira 404
    @Operation(summary = "Busca um piloto pelo id",
            description = "Retorna o piloto com a equipe completa e os links para a equipe (team) e os resultados dele (results)")
    @ApiResponse(responseCode = "200", description = "Piloto encontrado",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Driver.class)))
    @ApiResponse(responseCode = "404", description = "Piloto nao encontrado - nao existe piloto com esse id "
            + "(nunca foi criado ou ja foi excluido)", content = @Content)
    @ApiResponse(responseCode = "400", description = "Id invalido - o id precisa ser um numero inteiro (ex.: 1); "
            + "texto como abc e recusado", content = @Content)
    @GetMapping(path = "/drivers/{id}", version = "1")
    public EntityModel<Driver> getDriverById(@Parameter(description = "Id do piloto", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    // POST /drivers - o @RequestBody grandao e so doc do Swagger, o pequeno e que le o JSON
    // o @Valid roda as validacoes antes de entrar no metodo (400 se falhar)
    @Operation(summary = "Cria um piloto", description = "Cadastra um piloto novo. A equipe e obrigatoria e vai pelo id "
            + "(ex.: \"team\": { \"id\": 2 }); ela precisa existir antes. O numero do carro nao pode repetir entre os pilotos")
    @ApiResponse(responseCode = "201", description = "Piloto criado; o header Location traz o endereco dele")
    @ApiResponse(responseCode = "400", description = "Dados invalidos - name (3 a 100 caracteres), number (1 a 99), "
            + "nationality (2 a 60), birthDate (data no passado, no formato AAAA-MM-DD), worldTitles (a partir de 0) e team "
            + "sao obrigatorios. Tambem acontece com campo do tipo errado ou JSON mal formado; a resposta diz qual campo falhou e por que",
            content = @Content)
    @ApiResponse(responseCode = "404", description = "Equipe nao encontrada - o id informado em team nao existe; "
            + "crie a equipe antes ou corrija o id", content = @Content)
    @ApiResponse(responseCode = "409", description = "Numero repetido - outro piloto ja usa esse numero de carro", content = @Content)
    @PostMapping(path = "/drivers", version = "1")
    public ResponseEntity<EntityModel<Driver>> newDriver(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do piloto novo",
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
    @Operation(summary = "Atualiza um piloto", description = "Substitui todos os dados do piloto pelo que for enviado. "
            + "Para trocar o piloto de equipe, mande o id da equipe nova em team. Os resultados dele nao mudam")
    @ApiResponse(responseCode = "200", description = "Piloto atualizado",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Driver.class)))
    @ApiResponse(responseCode = "400", description = "Dados invalidos - o id nao e um numero inteiro, ou algum campo do JSON "
            + "e invalido (mesmas regras do POST)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Piloto ou equipe nao encontrado - nao existe piloto com esse id, "
            + "ou o id informado em team nao existe", content = @Content)
    @ApiResponse(responseCode = "409", description = "Numero repetido - outro piloto ja usa esse numero de carro", content = @Content)
    @PutMapping(path = "/drivers/{id}", version = "1")
    public ResponseEntity<EntityModel<Driver>> updateDriver(
            @Parameter(description = "Id do piloto", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados do piloto",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Driver.class),
                            examples = @ExampleObject(value = DRIVER_EXAMPLE)))
            @RequestBody @Valid Driver newDriver) {

        return ResponseEntity.ok(assembler.toModel(service.update(id, newDriver)));
    }

    // DELETE /drivers/{id} - sem versao, e igual na v1 e na v2
    @Operation(summary = "Exclui um piloto", description = "Apaga o piloto. Vale nas duas versoes. So da para excluir piloto "
            + "sem resultados: exclua os resultados dele antes, em DELETE /results/{id}")
    @ApiResponse(responseCode = "204", description = "Piloto excluido", content = @Content)
    @ApiResponse(responseCode = "404", description = "Piloto nao encontrado - nao existe piloto com esse id "
            + "(talvez ja tenha sido excluido)", content = @Content)
    @ApiResponse(responseCode = "409", description = "Piloto em uso - ele ainda tem resultados de corrida registrados", content = @Content)
    @DeleteMapping("/drivers/{id}")
    public ResponseEntity<Void> deleteDriver(@Parameter(description = "Id do piloto (o 9 e o criado no POST de exemplo)", example = "9") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /drivers/search?name= - busca por parte do nome
    @Operation(summary = "Busca pilotos pelo nome", description = "Consulta personalizada: pilotos cujo nome contem o texto informado, "
            + "em qualquer parte e sem diferenciar maiusculas (lando encontra Lando Norris)")
    @ApiResponse(responseCode = "200", description = "Lista paginada dos pilotos encontrados (vazia se nenhum nome bater)")
    @GetMapping("/drivers/search")
    public ResponseEntity<PagedModel<EntityModel<Driver>>> getDriversByName(
            @Parameter(description = "Parte do nome do piloto", example = "lando") @RequestParam String name,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.searchByName(name, pageable), assembler));
    }

    // GET /drivers/team/{teamId} - pilotos de uma equipe
    @Operation(summary = "Lista os pilotos de uma equipe", description = "Consulta personalizada (One-to-Many): pilotos que correm pela equipe")
    @ApiResponse(responseCode = "200", description = "Lista paginada dos pilotos da equipe")
    @ApiResponse(responseCode = "404", description = "Equipe nao encontrada - nao existe equipe com esse id", content = @Content)
    @GetMapping("/drivers/team/{teamId}")
    public ResponseEntity<PagedModel<EntityModel<Driver>>> getDriversByTeam(
            @Parameter(description = "Id da equipe", example = "1") @PathVariable Long teamId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByTeam(teamId, pageable), assembler));
    }

    // GET /drivers/standings - tabela do campeonato de pilotos
    @Operation(summary = "Classificacao de pilotos", description = "Consulta personalizada: tabela do campeonato de pilotos. "
            + "Soma os pontos de todos os resultados de cada piloto e ordena do maior para o menor; cada linha traz a posicao "
            + "e um link para o piloto (driver). A ordem e sempre por pontos, por isso a rota nao tem sort")
    @ApiResponse(responseCode = "200", description = "Tabela de classificacao paginada")
    // a tabela sempre ordena por pontos, entao no swagger so aparecem page e size (o sort seria ignorado)
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Numero da pagina, comecando em 0",
            schema = @Schema(type = "integer", defaultValue = "0"))
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "Quantos registros por pagina",
            schema = @Schema(type = "integer", defaultValue = "10"))
    @GetMapping("/drivers/standings")
    public ResponseEntity<PagedModel<EntityModel<Standing>>> getDriverStandings(
            @Parameter(hidden = true) @PageableDefault(size = 10, page = 0) Pageable pageable) {
        // cada linha da tabela ganha um link pro piloto
        return ResponseEntity.ok(standingsAssembler.toModel(service.standings(pageable), standing -> EntityModel.of(standing,
                linkTo(methodOn(DriverController.class).getDriverById(standing.id())).withRel("driver"))));
    }
}
