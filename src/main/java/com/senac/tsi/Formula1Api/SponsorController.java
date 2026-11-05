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

// endpoints dos patrocinadores, o mais simples de todos porque nao aponta pra ninguem
// pra ligar um patrocinador numa equipe usa o campo sponsors no POST/PUT de /teams
@RestController
@Tag(name = "Patrocinadores", description = "Patrocinadores das equipes. Um patrocinador pode apoiar varias equipes "
        + "e uma equipe pode ter varios patrocinadores (Many-to-Many)")
public class SponsorController {

    // exemplos do Swagger: o POST cria um registro novo e o PUT altera o id 1, que ja existe
    // (com o mesmo exemplo nos dois, o PUT dava 409 de valor repetido depois do POST)
    private static final String NEW_SPONSOR_EXAMPLE = """
            { "name": "Aramco", "industry": "Energy" }""";

    private static final String SPONSOR_EXAMPLE = """
            { "name": "Shell", "industry": "Energy" }""";

    private final SponsorService service;
    private final SponsorModelAssembler assembler;
    private final PagedResourcesAssembler<Sponsor> pagedResourcesAssembler;

    // injecao de dependencia pelo construtor
    public SponsorController(SponsorService service,
                             SponsorModelAssembler assembler,
                             PagedResourcesAssembler<Sponsor> pagedResourcesAssembler) {
        this.service = service;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    // GET /sponsors - lista paginada
    @Operation(summary = "Lista os patrocinadores", description = "Lista paginada de todos os patrocinadores. "
            + "Use page, size e sort para navegar e ordenar (ex.: sort=name,asc)")
    @ApiResponse(responseCode = "200", description = "Lista paginada de patrocinadores")
    @GetMapping("/sponsors")
    public ResponseEntity<PagedModel<EntityModel<Sponsor>>> getAllSponsors(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }

    // GET /sponsors/{id} - 404 se nao achar
    @Operation(summary = "Busca um patrocinador pelo id",
            description = "Retorna o patrocinador e o link para as equipes que ele patrocina (teams)")
    @ApiResponse(responseCode = "200", description = "Patrocinador encontrado",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Sponsor.class)))
    @ApiResponse(responseCode = "404", description = "Patrocinador nao encontrado - nao existe patrocinador com esse id "
            + "(nunca foi criado ou ja foi excluido)", content = @Content)
    @ApiResponse(responseCode = "400", description = "Id invalido - o id precisa ser um numero inteiro (ex.: 1); "
            + "texto como abc e recusado", content = @Content)
    @GetMapping("/sponsors/{id}")
    public EntityModel<Sponsor> getSponsorById(@Parameter(description = "Id do patrocinador", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    // POST /sponsors
    @Operation(summary = "Cria um patrocinador", description = "Cadastra um patrocinador novo, ainda sem equipe. "
            + "Para ligar ele a uma equipe, coloque o id dele na lista sponsors do POST ou PUT de /teams. O nome nao pode repetir")
    @ApiResponse(responseCode = "201", description = "Patrocinador criado; o header Location traz o endereco dele")
    @ApiResponse(responseCode = "400", description = "Dados invalidos - name (2 a 100 caracteres) e industry (2 a 60) "
            + "sao obrigatorios. Tambem acontece com campo do tipo errado ou JSON mal formado; "
            + "a resposta diz qual campo falhou e por que", content = @Content)
    @ApiResponse(responseCode = "409", description = "Nome repetido - ja existe um patrocinador com esse nome", content = @Content)
    @PostMapping("/sponsors")
    public ResponseEntity<EntityModel<Sponsor>> newSponsor(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do patrocinador novo",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Sponsor.class),
                            examples = @ExampleObject(value = NEW_SPONSOR_EXAMPLE)))
            @RequestBody @Valid Sponsor newSponsor) {

        EntityModel<Sponsor> entityModel = assembler.toModel(service.create(newSponsor));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    // PUT /sponsors/{id}
    @Operation(summary = "Atualiza um patrocinador", description = "Substitui o nome e o setor do patrocinador. "
            + "As equipes que ele patrocina nao mudam (isso e feito pela lista sponsors de /teams)")
    @ApiResponse(responseCode = "200", description = "Patrocinador atualizado",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Sponsor.class)))
    @ApiResponse(responseCode = "400", description = "Dados invalidos - o id nao e um numero inteiro, ou algum campo do JSON "
            + "e invalido (mesmas regras do POST)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Patrocinador nao encontrado - nao existe patrocinador com esse id", content = @Content)
    @ApiResponse(responseCode = "409", description = "Nome repetido - outro patrocinador ja usa esse nome", content = @Content)
    @PutMapping("/sponsors/{id}")
    public ResponseEntity<EntityModel<Sponsor>> updateSponsor(
            @Parameter(description = "Id do patrocinador", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados do patrocinador",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Sponsor.class),
                            examples = @ExampleObject(value = SPONSOR_EXAMPLE)))
            @RequestBody @Valid Sponsor newSponsor) {

        return ResponseEntity.ok(assembler.toModel(service.update(id, newSponsor)));
    }

    // DELETE /sponsors/{id} - o patrocinador sai de todas as equipes antes
    @Operation(summary = "Exclui um patrocinador", description = "Apaga o patrocinador e tira ele da lista sponsors "
            + "de todas as equipes que ele apoiava; as equipes continuam existindo")
    @ApiResponse(responseCode = "204", description = "Patrocinador excluido", content = @Content)
    @ApiResponse(responseCode = "404", description = "Patrocinador nao encontrado - nao existe patrocinador com esse id "
            + "(talvez ja tenha sido excluido)", content = @Content)
    @DeleteMapping("/sponsors/{id}")
    public ResponseEntity<Void> deleteSponsor(@Parameter(description = "Id do patrocinador (o 6 e o criado no POST de exemplo)", example = "6") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /sponsors/search?industry= - patrocinadores de um setor
    @Operation(summary = "Busca patrocinadores pelo setor", description = "Consulta personalizada: patrocinadores de um setor. "
            + "O setor precisa ser completo, mas maiusculas e minusculas nao importam (energy encontra Energy)")
    @ApiResponse(responseCode = "200", description = "Lista paginada dos patrocinadores do setor (vazia se nenhum for)")
    @GetMapping("/sponsors/search")
    public ResponseEntity<PagedModel<EntityModel<Sponsor>>> getSponsorsByIndustry(
            @Parameter(description = "Setor, completo e em ingles", example = "Energy") @RequestParam String industry,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.searchByIndustry(industry, pageable), assembler));
    }
}
