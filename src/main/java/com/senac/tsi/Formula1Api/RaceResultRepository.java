package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RaceResultRepository extends JpaRepository<RaceResult, Long> {

    Page<RaceResult> findByStatus(ResultStatus status, Pageable pageable);

    Page<RaceResult> findByRaceNameContainingIgnoreCase(String raceName, Pageable pageable);

    Page<RaceResult> findByDriverId(long driverId, Pageable pageable);
}
