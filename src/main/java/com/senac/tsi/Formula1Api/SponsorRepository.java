package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// acesso ao banco dos patrocinadores
@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, Long> {

    // patrocinadores de um setor, sem ligar pra maiuscula
    Page<Sponsor> findByIndustryIgnoreCase(String industry, Pageable pageable);
}
