package com.senac.tsi.Formula1Api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.Objects;

@Entity
@Schema(description = "Chefe de equipe (Team Principal)")
public class TeamPrincipal {
    private @Id
    @GeneratedValue long id;

    @NotBlank
    @Size(min = 3, max = 100)
    @Schema(example = "Andrea Stella")
    private String name;

    @NotBlank
    @Size(min = 2, max = 60)
    @Schema(example = "Italian")
    private String nationality;

    @NotNull
    @Min(1950)
    @Max(2100)
    @Schema(description = "Ano em que assumiu a equipe", example = "2023")
    private Integer since;

    // One-to-One: cada equipe tem um unico chefe (unique garante isso no banco)
    // No payload basta enviar o id: "team": { "id": 1 }
    @OneToOne
    @JoinColumn(name = "team_id", unique = true)
    private Team team;

    public TeamPrincipal() {
    }

    public TeamPrincipal(String name, String nationality, Integer since, Team team) {
        this.name = name;
        this.nationality = nationality;
        this.since = since;
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

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public Integer getSince() {
        return since;
    }

    public void setSince(Integer since) {
        this.since = since;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamPrincipal that = (TeamPrincipal) o;
        return id == that.id && Objects.equals(name, that.name) && Objects.equals(nationality, that.nationality) && Objects.equals(since, that.since);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, nationality, since);
    }

    @Override
    public String toString() {
        return "TeamPrincipal{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", nationality='" + nationality + '\'' +
                ", since=" + since +
                '}';
    }
}
