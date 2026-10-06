package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// acesso ao banco dos chefes de equipe
@Repository
public interface TeamPrincipalRepository extends JpaRepository<TeamPrincipal, Long> {

    // chefes de uma nacionalidade, sem ligar pra maiuscula
    Page<TeamPrincipal> findByNationalityIgnoreCase(String nationality, Pageable pageable);

    // volta Optional e nao Page porque cada equipe tem no maximo um chefe
    Optional<TeamPrincipal> findByTeamId(long teamId);
}
