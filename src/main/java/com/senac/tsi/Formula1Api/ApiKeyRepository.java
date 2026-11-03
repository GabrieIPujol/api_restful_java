package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// acesso ao banco das chaves de API
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    // usado pelo filtro pra achar a chave que veio no header (compara pelo hash)
    Optional<ApiKey> findByKeyHash(String keyHash);

    // chaves de um nivel (USER ou ADMIN)
    Page<ApiKey> findByRole(ApiKeyRole role, Pageable pageable);
}
