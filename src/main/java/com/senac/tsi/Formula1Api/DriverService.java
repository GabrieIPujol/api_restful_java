package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// regras de negocio dos pilotos, os controllers (v1 e v2) so cuidam do HTTP
@Service
public class DriverService {

    private final DriverRepository repository;
    private final TeamRepository teamRepository;

    public DriverService(DriverRepository repository, TeamRepository teamRepository) {
        this.repository = repository;
        this.teamRepository = teamRepository;
    }

    public Page<Driver> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    // nao achou vira 404 la no ApiExceptionAdvice
    public Driver findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException(id));
    }

    // a equipe chega so com id, aqui troca pela equipe de verdade antes de salvar
    public Driver create(Driver driver) {
        driver.setTeam(resolveTeam(driver.getTeam()));
        return repository.save(driver);
    }

    // como o piloto ja tem id, o save vira UPDATE
    public Driver update(long id, Driver data) {
        Driver driver = findById(id);
        driver.setName(data.getName());
        driver.setNumber(data.getNumber());
        driver.setNationality(data.getNationality());
        driver.setBirthDate(data.getBirthDate());
        driver.setWorldTitles(data.getWorldTitles());
        driver.setTeam(resolveTeam(data.getTeam()));
        return repository.save(driver);
    }

    // piloto com resultado nao pode sair, senao os resultados ficam orfaos
    @Transactional
    public void delete(long id) {
        Driver driver = findById(id);
        if (!driver.getResults().isEmpty())
            throw new ConflictException("Driver still has race results");
        repository.delete(driver);
    }

    public Page<Driver> searchByName(String name, Pageable pageable) {
        return repository.findByNameContainingIgnoreCase(name, pageable);
    }

    // sem essa checagem uma equipe que nao existe voltaria lista vazia em vez de 404
    public Page<Driver> findByTeam(long teamId, Pageable pageable) {
        if (!teamRepository.existsById(teamId))
            throw new TeamNotFoundException(teamId);
        return repository.findByTeamId(teamId, pageable);
    }

    // ignora o sort de quem chamou, a tabela sempre ordena por pontos
    public Page<Standing> standings(Pageable pageable) {
        return Standing.rank(repository.findDriverStandings(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())));
    }

    private Team resolveTeam(Team team) {
        return teamRepository.findById(team.getId())
                .orElseThrow(() -> new TeamNotFoundException(team.getId()));
    }
}
