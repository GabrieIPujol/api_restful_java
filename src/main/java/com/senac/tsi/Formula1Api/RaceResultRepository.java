package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// acesso ao banco dos resultados
@Repository
public interface RaceResultRepository extends JpaRepository<RaceResult, Long> {

    // filtra pelo enum (FINISHED, DNF ou DSQ)
    Page<RaceResult> findByStatus(ResultStatus status, Pageable pageable);

    // resultados de uma corrida pelo pedaco do nome, sem ligar pra maiuscula
    Page<RaceResult> findByRaceNameContainingIgnoreCase(String raceName, Pageable pageable);

    // historico de um piloto
    Page<RaceResult> findByDriverId(long driverId, Pageable pageable);
}
