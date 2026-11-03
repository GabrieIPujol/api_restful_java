package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// regras de negocio dos patrocinadores
@Service
public class SponsorService {

    private final SponsorRepository repository;

    public SponsorService(SponsorRepository repository) {
        this.repository = repository;
    }

    public Page<Sponsor> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Sponsor findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new SponsorNotFoundException(id));
    }

    public Sponsor create(Sponsor sponsor) {
        return repository.save(sponsor);
    }

    public Sponsor update(long id, Sponsor data) {
        Sponsor sponsor = findById(id);
        sponsor.setName(data.getName());
        sponsor.setIndustry(data.getIndustry());
        return repository.save(sponsor);
    }

    // primeiro tira o patrocinador de cada equipe (a equipe e a dona do vinculo), depois apaga
    @Transactional
    public void delete(long id) {
        Sponsor sponsor = findById(id);
        sponsor.getTeams().forEach(team -> team.getSponsors().remove(sponsor));
        repository.delete(sponsor);
    }

    public Page<Sponsor> searchByIndustry(String industry, Pageable pageable) {
        return repository.findByIndustryIgnoreCase(industry, pageable);
    }
}
