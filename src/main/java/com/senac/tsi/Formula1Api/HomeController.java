package com.senac.tsi.Formula1Api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@Tag(name = "Home", description = "Ponto de entrada da API")
public class HomeController {

    // Raiz navegavel: links para todas as colecoes da API
    @Operation(summary = "API entry point", description = "Retorna links HATEOAS para todos os recursos")
    @ApiResponse(responseCode = "200", description = "Links to every resource collection")
    @GetMapping("/")
    public RepresentationModel<?> index() {
        return new RepresentationModel<>()
                .add(linkTo(methodOn(TeamController.class).getAllTeams(Pageable.unpaged())).withRel("teams"))
                .add(linkTo(methodOn(TeamPrincipalController.class).getAllPrincipals(Pageable.unpaged())).withRel("principals"))
                .add(linkTo(methodOn(DriverController.class).getAllDrivers(Pageable.unpaged())).withRel("drivers"))
                .add(linkTo(methodOn(SponsorController.class).getAllSponsors(Pageable.unpaged())).withRel("sponsors"))
                .add(linkTo(methodOn(RaceResultController.class).getAllResults(Pageable.unpaged())).withRel("results"))
                .add(linkTo(methodOn(DriverController.class).getDriverStandings(Pageable.unpaged())).withRel("driverStandings"))
                .add(linkTo(methodOn(TeamController.class).getConstructorStandings(Pageable.unpaged())).withRel("constructorStandings"));
    }
}
