package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

// regras de negocio dos resultados, qualquer mudanca aqui ja muda as tabelas de classificacao
@Service
public class RaceResultService {

    private final RaceResultRepository repository;
    private final DriverRepository driverRepository;

    public RaceResultService(RaceResultRepository repository, DriverRepository driverRepository) {
        this.repository = repository;
        this.driverRepository = driverRepository;
    }

    public Page<RaceResult> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public RaceResult findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RaceResultNotFoundException(id));
    }

    // o piloto chega so com id, aqui troca pelo de verdade
    public RaceResult create(RaceResult result) {
        result.setDriver(resolveDriver(result.getDriver()));
        return repository.save(result);
    }

    public RaceResult update(long id, RaceResult data) {
        RaceResult result = findById(id);
        result.setRaceName(data.getRaceName());
        result.setCircuit(data.getCircuit());
        result.setRaceDate(data.getRaceDate());
        result.setPosition(data.getPosition());
        result.setPoints(data.getPoints());
        result.setStatus(data.getStatus());
        result.setDriver(resolveDriver(data.getDriver()));
        return repository.save(result);
    }

    // ninguem depende de resultado, entao aqui nao tem conflito
    public void delete(long id) {
        repository.delete(findById(id));
    }

    public Page<RaceResult> findByStatus(ResultStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable);
    }

    public Page<RaceResult> searchByRace(String race, Pageable pageable) {
        return repository.findByRaceNameContainingIgnoreCase(race, pageable);
    }

    public Page<RaceResult> findByDriver(long driverId, Pageable pageable) {
        if (!driverRepository.existsById(driverId))
            throw new DriverNotFoundException(driverId);
        return repository.findByDriverId(driverId, pageable);
    }

    private Driver resolveDriver(Driver driver) {
        return driverRepository.findById(driver.getId())
                .orElseThrow(() -> new DriverNotFoundException(driver.getId()));
    }
}
