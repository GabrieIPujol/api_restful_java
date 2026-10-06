package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamPrincipalRepository extends JpaRepository<TeamPrincipal, Long> {

    Page<TeamPrincipal> findByNationalityIgnoreCase(String nationality, Pageable pageable);

    Optional<TeamPrincipal> findByTeamId(long teamId);
}
