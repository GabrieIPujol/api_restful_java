package com.senac.tsi.Formula1Api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

// coloca os links do HATEOAS no chefe de equipe
@Component
class TeamPrincipalModelAssembler implements RepresentationModelAssembler<TeamPrincipal, EntityModel<TeamPrincipal>> {

    @Override
    public EntityModel<TeamPrincipal> toModel(TeamPrincipal principal) {
        EntityModel<TeamPrincipal> model = EntityModel.of(principal,
                linkTo(methodOn(TeamPrincipalController.class).getPrincipalById(principal.getId())).withSelfRel(),
                linkTo(methodOn(TeamPrincipalController.class).updatePrincipal(principal.getId(), null)).withRel("update"),
                linkTo(methodOn(TeamPrincipalController.class).deletePrincipal(principal.getId())).withRel("delete"),
                linkTo(methodOn(TeamPrincipalController.class).getAllPrincipals(Pageable.unpaged())).withRel("principals")
        );
        // o link da equipe so entra se o chefe tiver uma
        if (principal.getTeam() != null)
            model.add(linkTo(methodOn(TeamController.class).getTeamById(principal.getTeam().getId())).withRel("team"));
        return model;
    }
}
