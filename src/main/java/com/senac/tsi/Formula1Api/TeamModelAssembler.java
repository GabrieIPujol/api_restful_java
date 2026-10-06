package com.senac.tsi.Formula1Api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

// coloca os links do HATEOAS na equipe
@Component
class TeamModelAssembler implements RepresentationModelAssembler<Team, EntityModel<Team>> {

    // "drivers" e "principal" entram no lugar dos campos que tem @JsonIgnore
    @Override
    public EntityModel<Team> toModel(Team team) {
        return EntityModel.of(team,
                linkTo(methodOn(TeamController.class).getTeamById(team.getId())).withSelfRel(),
                linkTo(methodOn(TeamController.class).updateTeam(team.getId(), null)).withRel("update"),
                linkTo(methodOn(TeamController.class).deleteTeam(team.getId())).withRel("delete"),
                linkTo(methodOn(DriverController.class).getDriversByTeam(team.getId(), Pageable.unpaged())).withRel("drivers"),
                linkTo(methodOn(TeamPrincipalController.class).getPrincipalByTeam(team.getId())).withRel("principal"),
                linkTo(methodOn(TeamController.class).getAllTeams(Pageable.unpaged())).withRel("teams")
        );
    }
}
