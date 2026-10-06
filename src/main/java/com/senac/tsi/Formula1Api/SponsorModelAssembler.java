package com.senac.tsi.Formula1Api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class SponsorModelAssembler implements RepresentationModelAssembler<Sponsor, EntityModel<Sponsor>> {

    @Override
    public EntityModel<Sponsor> toModel(Sponsor sponsor) {
        return EntityModel.of(sponsor,
                linkTo(methodOn(SponsorController.class).getSponsorById(sponsor.getId())).withSelfRel(),
                linkTo(methodOn(SponsorController.class).updateSponsor(sponsor.getId(), null)).withRel("update"),
                linkTo(methodOn(SponsorController.class).deleteSponsor(sponsor.getId())).withRel("delete"),
                linkTo(methodOn(TeamController.class).getTeamsBySponsor(sponsor.getId(), Pageable.unpaged())).withRel("teams"),
                linkTo(methodOn(SponsorController.class).getAllSponsors(Pageable.unpaged())).withRel("sponsors")
        );
    }
}
