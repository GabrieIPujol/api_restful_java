package com.senac.tsi.Formula1Api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

import static com.senac.tsi.Formula1Api.ResultStatus.*;

// preenche o banco toda vez que a aplicacao sobe, ja que o H2 em memoria comeca vazio
@Configuration
public class LoadDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    // o CommandLineRunner roda uma vez logo depois que a aplicacao sobe
    @Bean
    CommandLineRunner initDatabase(TeamRepository teams, TeamPrincipalRepository principals, DriverRepository drivers,
                                   SponsorRepository sponsors, RaceResultRepository results) {
        return args -> {
            // patrocinadores primeiro, porque as equipes apontam pra eles
            Sponsor shell = sponsors.save(new Sponsor("Shell", "Energy"));
            Sponsor hp = sponsors.save(new Sponsor("HP", "Technology"));
            Sponsor oracle = sponsors.save(new Sponsor("Oracle", "Technology"));
            Sponsor petronas = sponsors.save(new Sponsor("Petronas", "Energy"));
            Sponsor google = sponsors.save(new Sponsor("Google", "Technology"));

            // equipes ja com os patrocinadores
            Team mclaren = new Team("McLaren", "United Kingdom", "Woking", 1963, 10);
            mclaren.getSponsors().add(google);
            Team ferrari = new Team("Ferrari", "Italy", "Maranello", 1929, 16);
            ferrari.getSponsors().add(shell);
            ferrari.getSponsors().add(hp);
            Team redBull = new Team("Red Bull Racing", "Austria", "Milton Keynes", 2005, 6);
            redBull.getSponsors().add(oracle);
            Team mercedes = new Team("Mercedes", "Germany", "Brackley", 2010, 8);
            mercedes.getSponsors().add(petronas);
            mercedes.getSponsors().add(google);
            mclaren = teams.save(mclaren);
            ferrari = teams.save(ferrari);
            redBull = teams.save(redBull);
            mercedes = teams.save(mercedes);
            log.info("Preloaded " + teams.count() + " teams");

            // um chefe pra cada equipe
            log.info("Preloading " + principals.save(new TeamPrincipal("Andrea Stella", "Italian", 2023, mclaren)));
            log.info("Preloading " + principals.save(new TeamPrincipal("Frederic Vasseur", "French", 2023, ferrari)));
            log.info("Preloading " + principals.save(new TeamPrincipal("Laurent Mekies", "French", 2025, redBull)));
            log.info("Preloading " + principals.save(new TeamPrincipal("Toto Wolff", "Austrian", 2013, mercedes)));

            // dois pilotos por equipe, os ids ficam de 1 a 8 nessa ordem
            Driver norris = drivers.save(new Driver("Lando Norris", 1, "British", LocalDate.of(1999, 11, 13), 1, mclaren));
            Driver piastri = drivers.save(new Driver("Oscar Piastri", 81, "Australian", LocalDate.of(2001, 4, 6), 0, mclaren));
            Driver leclerc = drivers.save(new Driver("Charles Leclerc", 16, "Monegasque", LocalDate.of(1997, 10, 16), 0, ferrari));
            Driver hamilton = drivers.save(new Driver("Lewis Hamilton", 44, "British", LocalDate.of(1985, 1, 7), 7, ferrari));
            Driver verstappen = drivers.save(new Driver("Max Verstappen", 3, "Dutch", LocalDate.of(1997, 9, 30), 4, redBull));
            Driver hadjar = drivers.save(new Driver("Isack Hadjar", 6, "French", LocalDate.of(2004, 9, 28), 0, redBull));
            Driver russell = drivers.save(new Driver("George Russell", 63, "British", LocalDate.of(1998, 2, 15), 0, mercedes));
            Driver antonelli = drivers.save(new Driver("Andrea Kimi Antonelli", 12, "Italian", LocalDate.of(2006, 8, 25), 0, mercedes));
            log.info("Preloaded " + drivers.count() + " drivers");

            // resultados de exemplo (nao sao oficiais), so pra tabela ter dados
            // australia: ids 1 a 8
            LocalDate australia = LocalDate.of(2026, 3, 8);
            results.save(new RaceResult("Australian Grand Prix", "Albert Park", australia, 1, 25, FINISHED, norris));
            results.save(new RaceResult("Australian Grand Prix", "Albert Park", australia, 2, 18, FINISHED, leclerc));
            results.save(new RaceResult("Australian Grand Prix", "Albert Park", australia, 3, 15, FINISHED, verstappen));
            results.save(new RaceResult("Australian Grand Prix", "Albert Park", australia, 4, 12, FINISHED, piastri));
            results.save(new RaceResult("Australian Grand Prix", "Albert Park", australia, 5, 10, FINISHED, russell));
            results.save(new RaceResult("Australian Grand Prix", "Albert Park", australia, 6, 8, FINISHED, hamilton));
            results.save(new RaceResult("Australian Grand Prix", "Albert Park", australia, 7, 6, FINISHED, hadjar));
            results.save(new RaceResult("Australian Grand Prix", "Albert Park", australia, 20, 0, DNF, antonelli));

            // china: ids 9 a 16
            LocalDate china = LocalDate.of(2026, 3, 15);
            results.save(new RaceResult("Chinese Grand Prix", "Shanghai International Circuit", china, 1, 25, FINISHED, piastri));
            results.save(new RaceResult("Chinese Grand Prix", "Shanghai International Circuit", china, 2, 18, FINISHED, verstappen));
            results.save(new RaceResult("Chinese Grand Prix", "Shanghai International Circuit", china, 3, 15, FINISHED, norris));
            results.save(new RaceResult("Chinese Grand Prix", "Shanghai International Circuit", china, 4, 12, FINISHED, russell));
            results.save(new RaceResult("Chinese Grand Prix", "Shanghai International Circuit", china, 5, 10, FINISHED, leclerc));
            results.save(new RaceResult("Chinese Grand Prix", "Shanghai International Circuit", china, 6, 8, FINISHED, antonelli));
            results.save(new RaceResult("Chinese Grand Prix", "Shanghai International Circuit", china, 7, 6, FINISHED, hamilton));
            results.save(new RaceResult("Chinese Grand Prix", "Shanghai International Circuit", china, 19, 0, DSQ, hadjar));
            log.info("Preloaded " + results.count() + " race results");
        };
    }
}
