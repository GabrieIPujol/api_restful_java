package com.senac.tsi.Formula1Api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
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

    // o que vem no corpo do GET /, alem dos links: quem chega aqui ja entende o que e a api e por onde seguir
    @Schema(description = "Apresentacao da API")
    public static class Home extends RepresentationModel<Home> {
        @Schema(example = "Formula 1 API")
        public final String name = "Formula 1 API";
        @Schema(example = OpenApi.VERSION)
        public final String version = OpenApi.VERSION;
        @Schema(example = "API de Formula 1: equipes, chefes de equipe, pilotos, patrocinadores, resultados e classificacoes...")
        public final String description = "API de Formula 1: equipes, chefes de equipe, pilotos, patrocinadores, resultados "
                + "e classificacoes. Siga os links abaixo para navegar; a documentacao completa esta no link docs. "
                + "Ler e livre; para criar, alterar ou excluir mande uma chave no header X-API-Key";
    }

    // GET / - apresenta a api e da os links pra documentacao, pras listas e pras tabelas
    @Operation(summary = "Porta de entrada da API", description = "Apresenta a API (nome, versao e o que ela faz) e retorna "
            + "os links para a documentacao (docs), os termos de uso, a licenca e todas as colecoes "
            + "(equipes, chefes, pilotos, patrocinadores, resultados, classificacoes e chaves). "
            + "E o ponto de partida do HATEOAS: daqui o cliente chega em tudo seguindo os links")
    @ApiResponse(responseCode = "200", description = "Apresentacao da API e os links para tudo")
    @GetMapping("/")
    public Home index() {
        return new Home()
                .add(linkTo(HomeController.class).slash("swagger-ui.html").withRel("docs"))
                .add(linkTo(HomeController.class).slash("terms.html").withRel("terms"))
                .add(linkTo(HomeController.class).slash("license.txt").withRel("license"))
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
