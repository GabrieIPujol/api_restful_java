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

// porta de entrada da API, daqui da pra navegar por tudo seguindo os links
@RestController
@Tag(name = "Inicio", description = "Porta de entrada da API")
public class HomeController {

    // GET / - nao devolve dado nenhum, so os links pras listas e pras tabelas
    @Operation(summary = "Porta de entrada da API", description = "Retorna os links para todas as colecoes da API "
            + "(equipes, chefes, pilotos, patrocinadores, resultados, classificacoes e chaves). "
            + "E o ponto de partida do HATEOAS: daqui o cliente chega em tudo seguindo os links")
    @ApiResponse(responseCode = "200", description = "Links para todas as colecoes")
    @GetMapping("/")
    public RepresentationModel<?> index() {
        return new RepresentationModel<>()
                .add(linkTo(methodOn(TeamController.class).getAllTeams(Pageable.unpaged())).withRel("teams"))
                .add(linkTo(methodOn(TeamPrincipalController.class).getAllPrincipals(Pageable.unpaged())).withRel("principals"))
                .add(linkTo(methodOn(DriverController.class).getAllDrivers(Pageable.unpaged())).withRel("drivers"))
                .add(linkTo(methodOn(SponsorController.class).getAllSponsors(Pageable.unpaged())).withRel("sponsors"))
                .add(linkTo(methodOn(RaceResultController.class).getAllResults(Pageable.unpaged())).withRel("results"))
                .add(linkTo(methodOn(DriverController.class).getDriverStandings(Pageable.unpaged())).withRel("driverStandings"))
                .add(linkTo(methodOn(TeamController.class).getConstructorStandings(Pageable.unpaged())).withRel("constructorStandings"))
                .add(linkTo(methodOn(ApiKeyController.class).getAllApiKeys(Pageable.unpaged())).withRel("apiKeys"));
    }
}
