package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// regras de negocio dos chefes de equipe
@Service
public class TeamPrincipalService {

    private final TeamPrincipalRepository repository;
    private final TeamRepository teamRepository;

    public TeamPrincipalService(TeamPrincipalRepository repository, TeamRepository teamRepository) {
        this.repository = repository;
        this.teamRepository = teamRepository;
    }

    public Page<TeamPrincipal> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public TeamPrincipal findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TeamPrincipalNotFoundException(id));
    }

    // se a equipe ja tiver chefe o unique do banco barra e volta 409
    public TeamPrincipal create(TeamPrincipal principal) {
        principal.setTeam(resolveTeam(principal.getTeam()));
        return repository.save(principal);
    }

    public TeamPrincipal update(long id, TeamPrincipal data) {
        TeamPrincipal principal = findById(id);
        principal.setName(data.getName());
        principal.setNationality(data.getNationality());
        principal.setSince(data.getSince());
        principal.setTeam(resolveTeam(data.getTeam()));
        return repository.save(principal);
    }

    // tira o chefe da equipe antes de apagar, senao o Hibernate reclama
    @Transactional
    public void delete(long id) {
        TeamPrincipal principal = findById(id);
        if (principal.getTeam() != null)
            principal.getTeam().setPrincipal(null);
        repository.delete(principal);
    }

    public Page<TeamPrincipal> searchByNationality(String nationality, Pageable pageable) {
        return repository.findByNationalityIgnoreCase(nationality, pageable);
    }

    // equipe que nao existe e equipe sem chefe, os dois voltam 404
    public TeamPrincipal findByTeam(long teamId) {
        if (!teamRepository.existsById(teamId))
            throw new TeamNotFoundException(teamId);
        return repository.findByTeamId(teamId)
                .orElseThrow(() -> new TeamPrincipalNotFoundException("Team " + teamId + " has no principal"));
    }

    // aqui aceita null porque o chefe pode ficar sem equipe
    private Team resolveTeam(Team team) {
        if (team == null)
            return null;
        return teamRepository.findById(team.getId())
                .orElseThrow(() -> new TeamNotFoundException(team.getId()));
    }
}
