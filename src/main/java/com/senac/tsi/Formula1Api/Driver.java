package com.senac.tsi.Formula1Api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Entity
@Schema(description = "Piloto de Formula 1")
public class Driver {
    private @Id
    @GeneratedValue long id;

    @NotBlank
    @Size(min = 3, max = 100)
    @Schema(example = "Lando Norris")
    private String name;

    @NotNull
    @Min(1)
    @Max(99)
    @Column(unique = true)
    @Schema(description = "Numero do carro", example = "4")
    private Integer number;

    @NotBlank
    @Size(min = 2, max = 60)
    @Schema(example = "British")
    private String nationality;

    @NotNull
    @Past
    @Schema(example = "1999-11-13")
    private LocalDate birthDate;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Titulos mundiais de pilotos", example = "1")
    private Integer worldTitles;

    // Many-to-One (lado dono do One-to-Many de Team)
    // No payload basta enviar o id: "team": { "id": 1 }
    @NotNull
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    // One-to-Many: lado inverso, navegavel pelo link "results"
    @JsonIgnore
    @OneToMany(mappedBy = "driver")
    private List<RaceResult> results;

    public Driver() {
    }

    public Driver(String name, Integer number, String nationality, LocalDate birthDate, Integer worldTitles, Team team) {
        this.name = name;
        this.number = number;
        this.nationality = nationality;
        this.birthDate = birthDate;
        this.worldTitles = worldTitles;
        this.team = team;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Integer getWorldTitles() {
        return worldTitles;
    }

    public void setWorldTitles(Integer worldTitles) {
        this.worldTitles = worldTitles;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public List<RaceResult> getResults() {
        return results;
    }

    public void setResults(List<RaceResult> results) {
        this.results = results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Driver driver = (Driver) o;
        return id == driver.id && Objects.equals(name, driver.name) && Objects.equals(number, driver.number) && Objects.equals(nationality, driver.nationality) && Objects.equals(birthDate, driver.birthDate) && Objects.equals(worldTitles, driver.worldTitles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, number, nationality, birthDate, worldTitles);
    }

    @Override
    public String toString() {
        return "Driver{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", number=" + number +
                ", nationality='" + nationality + '\'' +
                ", birthDate=" + birthDate +
                ", worldTitles=" + worldTitles +
                '}';
    }
}
