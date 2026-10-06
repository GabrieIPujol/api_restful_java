package com.senac.tsi.Formula1Api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

// coloca os links do HATEOAS no piloto (o "_links" do JSON)
@Component
class DriverModelAssembler implements RepresentationModelAssembler<Driver, EntityModel<Driver>> {

    // o linkTo(methodOn(...)) monta a URL olhando o mapeamento do controller,
    // entao se o caminho mudar la os links acompanham
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
