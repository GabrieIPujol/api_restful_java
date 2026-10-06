package com.senac.tsi.Formula1Api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

// essa classe vira a tabela DRIVER no banco, cada piloto e uma linha
@Entity
@Schema(description = "Piloto de Formula 1")
public class Driver {
    // id gerado pelo banco, por isso nao precisa mandar no POST
    private @Id
    @GeneratedValue long id;

    // as validacoes rodam no @Valid do controller, se falhar volta 400
    @NotBlank
    @Size(min = 3, max = 100)
    @Schema(example = "Lando Norris")
    private String name;

    // usei Integer em vez de int pra conseguir pegar o campo vazio com @NotNull
    // o unique e regra do banco, numero repetido volta 409
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

    // @Past garante que a data ta no passado
    @NotNull
    @Past
    @Schema(example = "1999-11-13")
    private LocalDate birthDate;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Titulos mundiais de pilotos", example = "1")
    private Integer worldTitles;

    // varios pilotos pra uma equipe, o piloto e o dono e guarda a coluna team_id
    // no JSON so precisa mandar o id: "team": { "id": 1 }
    @NotNull
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    // um piloto tem varios resultados, esse lado so serve pra navegar no java
    // o @JsonIgnore evita loop infinito no JSON, no lugar vai o link "results"
    @JsonIgnore
    @OneToMany(mappedBy = "driver")
    private List<RaceResult> results;

    // construtor vazio e obrigatorio pro JPA e pro Jackson
    public Driver() {
    }

    // usado no LoadDatabase, sem id porque quem gera e o banco
    public Driver(String name, Integer number, String nationality, LocalDate birthDate, Integer worldTitles, Team team) {
        this.name = name;
        this.number = number;
        this.nationality = nationality;
        this.birthDate = birthDate;
        this.worldTitles = worldTitles;
        this.team = team;
    }

    // o Jackson usa os getters pra montar o JSON e os setters pra ler o que chega
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

    // deixei os relacionamentos fora do equals/hashCode/toString pra nao dar loop
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
