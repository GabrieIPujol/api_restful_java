package com.senac.tsi.Formula1Api.v2;

import com.senac.tsi.Formula1Api.Driver;
import com.senac.tsi.Formula1Api.Team;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

// o que a v2 recebe: um DTO em vez da entidade, assim o banco pode mudar sem quebrar a api de novo
// mudanca da v2: a equipe vem como "teamId": 1 em vez de "team": { "id": 1 }
@Schema(description = "Piloto enviado na versao 2")
public record DriverRequest(
        @NotBlank @Size(min = 3, max = 100) @Schema(example = "Fernando Alonso") String name,
        @NotNull @Min(1) @Max(99) @Schema(example = "14") Integer number,
        @NotBlank @Size(min = 2, max = 60) @Schema(example = "Spanish") String nationality,
        @NotNull @Past @Schema(example = "1981-07-29") LocalDate birthDate,
        @NotNull @PositiveOrZero @Schema(example = "2") Integer worldTitles,
        @NotNull @Schema(example = "1") Long teamId) {

    // vira a entidade com uma equipe "vazia" so com o id, o DriverService troca pela de verdade
    Driver toEntity() {
        Team team = new Team();
        team.setId(teamId);
        return new Driver(name, number, nationality, birthDate, worldTitles, team);
    }
}
