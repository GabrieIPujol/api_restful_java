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
@Tag(name = "Sponsors", description = "Gerenciamento dos patrocinadores (Many-to-Many com Team)")
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
    @Operation(summary = "Get all sponsors", description = "Lista paginada de todos os patrocinadores")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of sponsors")
    @GetMapping("/sponsors")
    public ResponseEntity<PagedModel<EntityModel<Sponsor>>> getAllSponsors(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }

    // GET /sponsors/{id} - 404 se nao achar
    @Operation(summary = "Get a sponsor by its id", description = "Retorna um patrocinador com os links HATEOAS")
    @ApiResponse(responseCode = "200", description = "Returns a valid sponsor",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Sponsor.class)))
    @ApiResponse(responseCode = "404", description = "Sponsor not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping("/sponsors/{id}")
    public EntityModel<Sponsor> getSponsorById(@Parameter(description = "Id of the sponsor", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    // POST /sponsors
    @Operation(summary = "Creates a new sponsor", description = "Para vincular a uma equipe, use o campo sponsors no POST/PUT de /teams")
    @ApiResponse(responseCode = "201", description = "Sponsor created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "409", description = "A sponsor with this name already exists", content = @Content)
    @PostMapping("/sponsors")
    public ResponseEntity<EntityModel<Sponsor>> newSponsor(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New sponsor",
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
    @Operation(summary = "Updates a sponsor", description = "Substitui todos os dados do patrocinador")
    @ApiResponse(responseCode = "200", description = "Sponsor updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Sponsor.class)))
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Sponsor not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "A sponsor with this name already exists", content = @Content)
    @PutMapping("/sponsors/{id}")
    public ResponseEntity<EntityModel<Sponsor>> updateSponsor(
            @Parameter(description = "Id of the sponsor", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated sponsor",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Sponsor.class),
                            examples = @ExampleObject(value = SPONSOR_EXAMPLE)))
            @RequestBody @Valid Sponsor newSponsor) {

        return ResponseEntity.ok(assembler.toModel(service.update(id, newSponsor)));
    }

    // DELETE /sponsors/{id} - o patrocinador sai de todas as equipes antes
    @Operation(summary = "Deletes a sponsor", description = "Tambem remove o vinculo do patrocinador com as equipes")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a sponsor", content = @Content)
    @ApiResponse(responseCode = "404", description = "Sponsor not found, maybe it's already deleted", content = @Content)
    @DeleteMapping("/sponsors/{id}")
    public ResponseEntity<Void> deleteSponsor(@Parameter(description = "Id of the sponsor (o 6 e o criado no POST de exemplo)", example = "6") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /sponsors/search?industry= - patrocinadores de um setor
    @Operation(summary = "Search sponsors by industry", description = "Consulta personalizada: patrocinadores de um setor (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of sponsors")
    @GetMapping("/sponsors/search")
    public ResponseEntity<PagedModel<EntityModel<Sponsor>>> getSponsorsByIndustry(
            @Parameter(description = "Industry", example = "Energy") @RequestParam String industry,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.searchByIndustry(industry, pageable), assembler));
    }
}
