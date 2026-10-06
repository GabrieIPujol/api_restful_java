package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    Page<Driver> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Driver> findByTeamId(long teamId, Pageable pageable);

    // Campeonato de pilotos: soma dos pontos de cada piloto
    @Query(value = "select new com.senac.tsi.Formula1Api.Standing(d.id, d.name, sum(r.points)) " +
            "from RaceResult r join r.driver d " +
            "group by d.id, d.name order by sum(r.points) desc, d.name",
            countQuery = "select count(distinct r.driver) from RaceResult r")
    Page<Standing> findDriverStandings(Pageable pageable);
}
