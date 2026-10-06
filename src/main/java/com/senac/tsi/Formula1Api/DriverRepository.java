package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

// acesso ao banco dos pilotos, o Spring Data monta a implementacao sozinho
// so de estender o JpaRepository ja ganha findAll, findById, save, delete...
@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    // o Spring le o nome do metodo e gera o SQL: nome contem o texto, sem ligar pra maiuscula
    Page<Driver> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // pilotos de uma equipe (WHERE team_id = ?)
    Page<Driver> findByTeamId(long teamId, Pageable pageable);

    // tabela de pilotos: soma os pontos de cada um e ordena do maior pro menor
    // o countQuery conta os pilotos pra paginacao, com group by o Spring nao consegue sozinho
    @Query(value = "select new com.senac.tsi.Formula1Api.Standing(d.id, d.name, sum(r.points)) " +
            "from RaceResult r join r.driver d " +
            "group by d.id, d.name order by sum(r.points) desc, d.name",
            countQuery = "select count(distinct r.driver) from RaceResult r")
    Page<Standing> findDriverStandings(Pageable pageable);
}
