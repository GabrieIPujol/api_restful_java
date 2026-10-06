package com.senac.tsi.Formula1Api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class RaceResultModelAssembler implements RepresentationModelAssembler<RaceResult, EntityModel<RaceResult>> {

    @Override
    public EntityModel<RaceResult> toModel(RaceResult result) {
        return EntityModel.of(result,
                linkTo(methodOn(RaceResultController.class).getResultById(result.getId())).withSelfRel(),
                linkTo(methodOn(RaceResultController.class).updateResult(result.getId(), null)).withRel("update"),
                linkTo(methodOn(RaceResultController.class).deleteResult(result.getId())).withRel("delete"),
                linkTo(methodOn(DriverController.class).getDriverById(result.getDriver().getId())).withRel("driver"),
                linkTo(methodOn(RaceResultController.class).getAllResults(Pageable.unpaged())).withRel("results")
        );
    }
}
