package com.senac.tsi.Formula1Api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

// essa classe vira a tabela API_KEY, cada linha e uma chave que pode ir no header X-API-Key
// a chave de verdade nunca fica salva, so o hash dela (igual senha)
@Entity
@Schema(description = "Chave de API usada no header X-API-Key")
public class ApiKey {
    // id gerado pelo banco
    private @Id
    @GeneratedValue long id;

    // pra saber de quem e a chave, ex.: "front-end da equipe"
    @NotBlank
    @Size(min = 3, max = 100)
    @Schema(example = "Front-end da equipe")
    private String name;

    // nivel de acesso, gravado pelo nome no banco
    @NotNull
    @Enumerated(EnumType.STRING)
    @Schema(example = "USER")
    private ApiKeyRole role;

    // false desativa a chave sem apagar, comeca true se nao mandar
    @Schema(description = "Se false, a chave para de funcionar", example = "true")
    private boolean active = true;

    // hash SHA-256 da chave, nunca aparece no JSON
    @JsonIgnore
    @Column(unique = true, nullable = false)
    private String keyHash;

    // comecinho da chave, so pra pessoa reconhecer qual e
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Inicio da chave, so para identificar", example = "f1_Xk2p")
    private String keyPrefix;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Instant createdAt;

    // a chave aberta, nao vai pro banco e so aparece uma vez, na resposta da criacao
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "A chave completa: aparece so na resposta da criacao, guarde ela")
    private String key;

    // construtor vazio e obrigatorio pro JPA e pro Jackson
    public ApiKey() {
    }

    public ApiKey(String name, ApiKeyRole role, String keyHash, String keyPrefix, Instant createdAt) {
        this.name = name;
        this.role = role;
        this.keyHash = keyHash;
        this.keyPrefix = keyPrefix;
        this.createdAt = createdAt;
    }

    // getters e setters, o Jackson usa pra montar e ler o JSON
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ApiKeyRole getRole() {
        return role;
    }

    public void setRole(ApiKeyRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    // o hash e a chave ficam fora daqui pra nao vazar em log
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiKey apiKey = (ApiKey) o;
        return id == apiKey.id && active == apiKey.active && Objects.equals(name, apiKey.name) && role == apiKey.role && Objects.equals(keyPrefix, apiKey.keyPrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, role, active, keyPrefix);
    }

    @Override
    public String toString() {
        return "ApiKey{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", role=" + role +
                ", active=" + active +
                ", keyPrefix='" + keyPrefix + '\'' +
                '}';
    }
}
