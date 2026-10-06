package com.senac.tsi.Formula1Api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// essa classe vira a tabela SPONSOR, que guarda os patrocinadores
@Entity
@Schema(description = "Patrocinador de equipes")
public class Sponsor {
    // id gerado pelo banco
    private @Id
    @GeneratedValue long id;

    // validacoes rodam no @Valid (400), o unique e do banco (nome repetido da 409)
    @NotBlank
    @Size(min = 2, max = 100)
    @Column(unique = true)
    @Schema(example = "Shell")
    private String name;

    @NotBlank
    @Size(min = 2, max = 60)
    @Schema(description = "Setor de atuacao", example = "Energy")
    private String industry;

    // lado inverso do many-to-many, quem cuida da tabela TEAM_SPONSOR e a equipe
    // @JsonIgnore pra nao dar loop, no lugar vai o link "teams"
    @JsonIgnore
    @ManyToMany(mappedBy = "sponsors")
    private Set<Team> teams = new HashSet<>();

    // construtor vazio e obrigatorio pro JPA e pro Jackson
    public Sponsor() {
    }

    // usado no LoadDatabase
    public Sponsor(String name, String industry) {
        this.name = name;
        this.industry = industry;
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

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public Set<Team> getTeams() {
        return teams;
    }

    public void setTeams(Set<Team> teams) {
        this.teams = teams;
    }

    // as equipes ficam de fora daqui pra nao dar loop
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sponsor sponsor = (Sponsor) o;
        return id == sponsor.id && Objects.equals(name, sponsor.name) && Objects.equals(industry, sponsor.industry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, industry);
    }

    @Override
    public String toString() {
        return "Sponsor{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", industry='" + industry + '\'' +
                '}';
    }
}
