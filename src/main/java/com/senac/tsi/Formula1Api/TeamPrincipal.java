package com.senac.tsi.Formula1Api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.Objects;

// essa classe vira a tabela TEAM_PRINCIPAL, que guarda os chefes de equipe
@Entity
@Schema(description = "Chefe de equipe (Team Principal)")
public class TeamPrincipal {
    // id gerado pelo banco
    private @Id
    @GeneratedValue long id;

    // validacoes rodam no @Valid, se falhar volta 400
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

    // one-to-one: o chefe guarda a coluna team_id, e o unique impede dois chefes na mesma equipe (409)
    // nao tem @NotNull porque o chefe pode ficar sem equipe
    @OneToOne
    @JoinColumn(name = "team_id", unique = true)
    private Team team;

    // construtor vazio e obrigatorio pro JPA e pro Jackson
    public TeamPrincipal() {
    }

    // usado no LoadDatabase
    public TeamPrincipal(String name, String nationality, Integer since, Team team) {
        this.name = name;
        this.nationality = nationality;
        this.since = since;
        this.team = team;
    }

    // getters e setters, o Jackson usa pra montar e ler o JSON
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

    // a equipe fica de fora daqui pra nao dar loop
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
