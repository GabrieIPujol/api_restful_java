package com.senac.tsi.Formula1Api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Schema(description = "Resultado de um piloto em um Grande Premio")
public class RaceResult {
    private @Id
    @GeneratedValue long id;

    @NotBlank
    @Size(min = 3, max = 100)
    @Schema(example = "Australian Grand Prix")
    private String raceName;

    @NotBlank
    @Size(min = 3, max = 100)
    @Schema(example = "Albert Park")
    private String circuit;

    @NotNull
    @Schema(example = "2026-03-08")
    private LocalDate raceDate;

    @NotNull
    @Min(1)
    @Max(30)
    @Schema(description = "Posicao final na corrida", example = "1")
    private Integer position;

    @NotNull
    @PositiveOrZero
    @Max(26)
    @Schema(description = "Pontos conquistados (25 para o vencedor, +1 volta mais rapida)", example = "25")
    private Integer points;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Schema(example = "FINISHED")
    private ResultStatus status;

    // Many-to-One: um piloto tem varios resultados
    // No payload basta enviar o id: "driver": { "id": 1 }
    @NotNull
    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    public RaceResult() {
    }

    public RaceResult(String raceName, String circuit, LocalDate raceDate, Integer position, Integer points, ResultStatus status, Driver driver) {
        this.raceName = raceName;
        this.circuit = circuit;
        this.raceDate = raceDate;
        this.position = position;
        this.points = points;
        this.status = status;
        this.driver = driver;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public String getCircuit() {
        return circuit;
    }

    public void setCircuit(String circuit) {
        this.circuit = circuit;
    }

    public LocalDate getRaceDate() {
        return raceDate;
    }

    public void setRaceDate(LocalDate raceDate) {
        this.raceDate = raceDate;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public ResultStatus getStatus() {
        return status;
    }

    public void setStatus(ResultStatus status) {
        this.status = status;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RaceResult that = (RaceResult) o;
        return id == that.id && Objects.equals(raceName, that.raceName) && Objects.equals(circuit, that.circuit) && Objects.equals(raceDate, that.raceDate) && Objects.equals(position, that.position) && Objects.equals(points, that.points) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, raceName, circuit, raceDate, position, points, status);
    }

    @Override
    public String toString() {
        return "RaceResult{" +
                "id=" + id +
                ", raceName='" + raceName + '\'' +
                ", circuit='" + circuit + '\'' +
                ", raceDate=" + raceDate +
                ", position=" + position +
                ", points=" + points +
                ", status=" + status +
                '}';
    }
}
