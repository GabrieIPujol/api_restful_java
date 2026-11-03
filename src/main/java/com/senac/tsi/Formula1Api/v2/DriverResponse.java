package com.senac.tsi.Formula1Api.v2;

import com.senac.tsi.Formula1Api.Driver;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.Period;

// o que a v2 devolve: a equipe vem resumida (so id e nome) e ganha a idade calculada
@Schema(description = "Piloto devolvido na versao 2")
public record DriverResponse(
        @Schema(example = "1") long id,
        @Schema(example = "Lando Norris") String name,
        @Schema(example = "1") int number,
        @Schema(example = "British") String nationality,
        @Schema(example = "1999-11-13") LocalDate birthDate,
        @Schema(description = "Idade calculada a partir da data de nascimento", example = "26") int age,
        @Schema(example = "1") int worldTitles,
        TeamSummary team) {

    static DriverResponse from(Driver driver) {
        return new DriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getNumber(),
                driver.getNationality(),
                driver.getBirthDate(),
                Period.between(driver.getBirthDate(), LocalDate.now()).getYears(),
                driver.getWorldTitles(),
                new TeamSummary(driver.getTeam().getId(), driver.getTeam().getName()));
    }
}
