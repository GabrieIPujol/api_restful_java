package com.senac.tsi.Formula1Api;

// niveis de acesso das chaves de API
public enum ApiKeyRole {
    // pode cadastrar, alterar e apagar dados (POST, PUT, DELETE)
    USER,
    // tudo que o USER faz e ainda gerencia as chaves em /api-keys
    ADMIN
}
