package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

// acesso ao banco das equipes, o Spring Data monta a implementacao sozinho
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    // equipes de um pais, sem ligar pra maiuscula
    Page<Team> findByCountryIgnoreCase(String country, Pageable pageable);

    // equipes que tem esse patrocinador (o Spring faz o join com a TEAM_SPONSOR)
    Page<Team> findBySponsorsId(long sponsorId, Pageable pageable);

    // tabela de construtores: soma os pontos de todos os pilotos de cada equipe
    // o countQuery conta as equipes pra paginacao
    @Query(value = "select new com.senac.tsi.Formula1Api.Standing(t.id, t.name, sum(r.points)) " +
            "from RaceResult r join r.driver d join d.team t " +
            "group by t.id, t.name order by sum(r.points) desc, t.name",
            countQuery = "select count(distinct d.team) from RaceResult r join r.driver d")
    Page<Standing> findConstructorStandings(Pageable pageable);
}
