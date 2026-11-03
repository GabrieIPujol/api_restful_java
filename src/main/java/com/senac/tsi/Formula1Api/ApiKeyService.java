package com.senac.tsi.Formula1Api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

// regras das chaves de API: gerar, validar e gerenciar
@Service
public class ApiKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository repository;

    public ApiKeyService(ApiKeyRepository repository) {
        this.repository = repository;
    }

    // usado pelo ApiKeyFilter: acha a chave pelo hash e so aceita se estiver ativa
    public Optional<ApiKey> authenticate(String rawKey) {
        return repository.findByKeyHash(hash(rawKey)).filter(ApiKey::isActive);
    }

    public Page<ApiKey> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public ApiKey findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiKeyNotFoundException(id));
    }

    public Page<ApiKey> findByRole(ApiKeyRole role, Pageable pageable) {
        return repository.findByRole(role, pageable);
    }

    // gera uma chave aleatoria (f1_ + 32 caracteres) e devolve ela aberta so dessa vez
    public ApiKey create(ApiKey data) {
        byte[] random = new byte[24];
        RANDOM.nextBytes(random);
        String rawKey = "f1_" + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        return save(data.getName(), data.getRole(), rawKey);
    }

    // muda nome, nivel e se ta ativa, a chave em si nunca muda
    // nao deixa desativar a propria chave, senao a pessoa se tranca pra fora
    public ApiKey update(long id, ApiKey data, long currentKeyId) {
        ApiKey apiKey = findById(id);
        if (id == currentKeyId && !data.isActive())
            throw new ConflictException("You cannot deactivate the API key you are using");
        apiKey.setName(data.getName());
        apiKey.setRole(data.getRole());
        apiKey.setActive(data.isActive());
        return repository.save(apiKey);
    }

    // apagar = revogar, a chave para de funcionar na hora
    public void delete(long id, long currentKeyId) {
        ApiKey apiKey = findById(id);
        if (id == currentKeyId)
            throw new ConflictException("You cannot revoke the API key you are using");
        repository.delete(apiKey);
    }

    // usado no LoadDatabase pra criar as chaves que vem da configuracao
    public void ensureKey(String name, ApiKeyRole role, String rawKey) {
        if (rawKey != null && !rawKey.isBlank() && repository.findByKeyHash(hash(rawKey)).isEmpty())
            save(name, role, rawKey);
    }

    private ApiKey save(String name, ApiKeyRole role, String rawKey) {
        ApiKey apiKey = new ApiKey(name, role, hash(rawKey), rawKey.substring(0, Math.min(7, rawKey.length())), Instant.now());
        ApiKey saved = repository.save(apiKey);
        // campo @Transient, nao vai pro banco, so pra resposta mostrar a chave uma vez
        saved.setKey(rawKey);
        return saved;
    }

    private static String hash(String rawKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
