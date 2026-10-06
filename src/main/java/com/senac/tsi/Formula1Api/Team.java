package com.senac.tsi.Formula1Api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Schema(description = "Equipe (construtor) de Formula 1")
public class Team {
    private @Id
    @GeneratedValue long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(unique = true)
    @Schema(example = "McLaren")
    private String name;

    @NotBlank
    @Size(min = 2, max = 60)
    @Schema(example = "United Kingdom")
    private String country;

    @Size(max = 100)
    @Schema(example = "Woking")
    private String base;

    @NotNull
    @Min(1900)
    @Max(2100)
    @Schema(example = "1963")
    private Integer foundedYear;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Titulos mundiais de construtores", example = "10")
    private Integer constructorTitles;

    // Many-to-Many: uma equipe tem varios patrocinadores e um patrocinador pode patrocinar varias equipes
    // No payload basta enviar os ids: "sponsors": [{ "id": 1 }]
    @ManyToMany
    @JoinTable(name = "team_sponsor",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "sponsor_id"))
    private Set<Sponsor> sponsors = new HashSet<>();

    // One-to-Many: lado inverso, navegavel pelo link "drivers"
    @JsonIgnore
    @OneToMany(mappedBy = "team")
    private List<Driver> drivers;

    // One-to-One: lado inverso, navegavel pelo link "principal"
    @JsonIgnore
    @OneToOne(mappedBy = "team")
    private TeamPrincipal principal;

    public Team() {
    }

    public Team(String name, String country, String base, Integer foundedYear, Integer constructorTitles) {
        this.name = name;
        this.country = country;
        this.base = base;
        this.foundedYear = foundedYear;
        this.constructorTitles = constructorTitles;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public Integer getFoundedYear() {
        return foundedYear;
    }

    public void setFoundedYear(Integer foundedYear) {
        this.foundedYear = foundedYear;
    }

    public Integer getConstructorTitles() {
        return constructorTitles;
    }

    public void setConstructorTitles(Integer constructorTitles) {
        this.constructorTitles = constructorTitles;
    }

    public Set<Sponsor> getSponsors() {
        return sponsors;
    }

    public void setSponsors(Set<Sponsor> sponsors) {
        this.sponsors = sponsors;
    }

    public List<Driver> getDrivers() {
        return drivers;
    }

    public void setDrivers(List<Driver> drivers) {
        this.drivers = drivers;
    }

    public TeamPrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(TeamPrincipal principal) {
        this.principal = principal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return id == team.id && Objects.equals(name, team.name) && Objects.equals(country, team.country) && Objects.equals(base, team.base) && Objects.equals(foundedYear, team.foundedYear) && Objects.equals(constructorTitles, team.constructorTitles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, country, base, foundedYear, constructorTitles);
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", base='" + base + '\'' +
                ", foundedYear=" + foundedYear +
                ", constructorTitles=" + constructorTitles +
                '}';
    }
}
