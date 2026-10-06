package com.senac.tsi.Formula1Api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Schema(description = "Patrocinador de equipes")
public class Sponsor {
    private @Id
    @GeneratedValue long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(unique = true)
    @Schema(example = "Shell")
    private String name;

    @NotBlank
    @Size(min = 2, max = 60)
    @Schema(description = "Setor de atuacao", example = "Energy")
    private String industry;

    // Many-to-Many: lado inverso, navegavel pelo link "teams"
    @JsonIgnore
    @ManyToMany(mappedBy = "sponsors")
    private Set<Team> teams = new HashSet<>();

    public Sponsor() {
    }

    public Sponsor(String name, String industry) {
        this.name = name;
        this.industry = industry;
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
