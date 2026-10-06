package com.senac.tsi.Formula1Api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class DriverModelAssembler implements RepresentationModelAssembler<Driver, EntityModel<Driver>> {

    @Override
    public EntityModel<Driver> toModel(Driver driver) {
        return EntityModel.of(driver,
                linkTo(methodOn(DriverController.class).getDriverById(driver.getId())).withSelfRel(),
                linkTo(methodOn(DriverController.class).updateDriver(driver.getId(), null)).withRel("update"),
                linkTo(methodOn(DriverController.class).deleteDriver(driver.getId())).withRel("delete"),
                linkTo(methodOn(TeamController.class).getTeamById(driver.getTeam().getId())).withRel("team"),
                linkTo(methodOn(RaceResultController.class).getResultsByDriver(driver.getId(), Pageable.unpaged())).withRel("results"),
                linkTo(methodOn(DriverController.class).getAllDrivers(Pageable.unpaged())).withRel("drivers")
        );
    }
}
