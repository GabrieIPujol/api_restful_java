package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    Page<Team> findByCountryIgnoreCase(String country, Pageable pageable);

    Page<Team> findBySponsorsId(long sponsorId, Pageable pageable);

    // Campeonato de construtores: soma dos pontos de todos os pilotos da equipe
    @Query(value = "select new com.senac.tsi.Formula1Api.Standing(t.id, t.name, sum(r.points)) " +
            "from RaceResult r join r.driver d join d.team t " +
            "group by t.id, t.name order by sum(r.points) desc, t.name",
            countQuery = "select count(distinct d.team) from RaceResult r join r.driver d")
    Page<Standing> findConstructorStandings(Pageable pageable);
}
