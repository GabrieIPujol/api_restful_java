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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Sponsors", description = "Gerenciamento dos patrocinadores (Many-to-Many com Team)")
public class SponsorController {

    private static final String SPONSOR_EXAMPLE = """
            { "name": "Shell", "industry": "Energy" }""";

    private final SponsorRepository repository;
    private final SponsorModelAssembler assembler;
    private final PagedResourcesAssembler<Sponsor> pagedResourcesAssembler;

    public SponsorController(SponsorRepository repository,
                             SponsorModelAssembler assembler,
                             PagedResourcesAssembler<Sponsor> pagedResourcesAssembler) {
        this.repository = repository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all sponsors", description = "Lista paginada de todos os patrocinadores")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of sponsors")
    @GetMapping("/sponsors")
    public ResponseEntity<PagedModel<EntityModel<Sponsor>>> getAllSponsors(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Sponsor> sponsorPage = repository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(sponsorPage, assembler));
    }

    @Operation(summary = "Get a sponsor by its id")
    @ApiResponse(responseCode = "200", description = "Returns a valid sponsor",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Sponsor.class)))
    @ApiResponse(responseCode = "404", description = "Sponsor not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping("/sponsors/{id}")
    public EntityModel<Sponsor> getSponsorById(@Parameter(description = "Id of the sponsor", example = "1") @PathVariable Long id) {
        Sponsor sponsor = repository.findById(id)
                .orElseThrow(() -> new SponsorNotFoundException(id));
        return assembler.toModel(sponsor);
    }

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
                            examples = @ExampleObject(value = SPONSOR_EXAMPLE)))
            @RequestBody @Valid Sponsor newSponsor) {

        EntityModel<Sponsor> entityModel = assembler.toModel(repository.save(newSponsor));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    @Operation(summary = "Updates a sponsor")
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

        Sponsor updated = repository.findById(id)
                .map(sponsor -> {
                    sponsor.setName(newSponsor.getName());
                    sponsor.setIndustry(newSponsor.getIndustry());
                    return repository.save(sponsor);
                })
                .orElseThrow(() -> new SponsorNotFoundException(id));

        return ResponseEntity.ok(assembler.toModel(updated));
    }

    @Operation(summary = "Deletes a sponsor", description = "Tambem remove o vinculo do patrocinador com as equipes")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a sponsor", content = @Content)
    @ApiResponse(responseCode = "404", description = "Sponsor not found, maybe it's already deleted", content = @Content)
    @Transactional
    @DeleteMapping("/sponsors/{id}")
    public ResponseEntity<?> deleteSponsor(@Parameter(description = "Id of the sponsor", example = "1") @PathVariable Long id) {
        var sponsor = repository.findById(id);
        if (sponsor.isEmpty())
            return ResponseEntity.notFound().build();

        // Team e o dono do Many-to-Many, entao o vinculo e removido pelo lado da equipe
        sponsor.get().getTeams().forEach(team -> team.getSponsors().remove(sponsor.get()));
        repository.delete(sponsor.get());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search sponsors by industry", description = "Consulta personalizada: patrocinadores de um setor (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of sponsors")
    @GetMapping("/sponsors/search")
    public ResponseEntity<PagedModel<EntityModel<Sponsor>>> getSponsorsByIndustry(
            @Parameter(description = "Industry", example = "Energy") @RequestParam String industry,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Sponsor> sponsorPage = repository.findByIndustryIgnoreCase(industry, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(sponsorPage, assembler));
    }
}
