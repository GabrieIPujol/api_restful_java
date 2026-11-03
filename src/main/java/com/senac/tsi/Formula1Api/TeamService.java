package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

// regras de negocio das equipes
@Service
public class TeamService {

    private final TeamRepository repository;
    private final SponsorRepository sponsorRepository;

    public TeamService(TeamRepository repository, SponsorRepository sponsorRepository) {
        this.repository = repository;
        this.sponsorRepository = sponsorRepository;
    }

    public Page<Team> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Team findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));
    }

    // os patrocinadores chegam so com id, aqui busca os de verdade
    public Team create(Team team) {
        team.setSponsors(resolveSponsors(team.getSponsors()));
        return repository.save(team);
    }

    // substitui tudo, inclusive a lista de patrocinadores
    public Team update(long id, Team data) {
        Team team = findById(id);
        team.setName(data.getName());
        team.setCountry(data.getCountry());
        team.setBase(data.getBase());
        team.setFoundedYear(data.getFoundedYear());
        team.setConstructorTitles(data.getConstructorTitles());
        team.setSponsors(resolveSponsors(data.getSponsors()));
        return repository.save(team);
    }

    // se ainda tem piloto ou chefe nao deixa apagar
    // os vinculos com patrocinador saem junto porque a equipe e a dona
    @Transactional
    public void delete(long id) {
        Team team = findById(id);
        if (!team.getDrivers().isEmpty() || team.getPrincipal() != null)
            throw new ConflictException("Team still has drivers or a principal");
        repository.delete(team);
    }

    public Page<Team> searchByCountry(String country, Pageable pageable) {
        return repository.findByCountryIgnoreCase(country, pageable);
    }

    public Page<Team> findBySponsor(long sponsorId, Pageable pageable) {
        if (!sponsorRepository.existsById(sponsorId))
            throw new SponsorNotFoundException(sponsorId);
        return repository.findBySponsorsId(sponsorId, pageable);
    }

    // tabela de construtores, sempre ordenada por pontos
    public Page<Standing> standings(Pageable pageable) {
        return Standing.rank(repository.findConstructorStandings(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())));
    }

    // troca cada patrocinador que veio so com id pelo do banco (404 se algum nao existir)
    private Set<Sponsor> resolveSponsors(Set<Sponsor> sponsors) {
        Set<Sponsor> resolved = new HashSet<>();
        if (sponsors == null)
            return resolved;

        for (Sponsor sponsor : sponsors)
            resolved.add(sponsorRepository.findById(sponsor.getId())
                    .orElseThrow(() -> new SponsorNotFoundException(sponsor.getId())));
        return resolved;
    }
}
