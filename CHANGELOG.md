# Changelog

Todas as mudanças da Formula 1 API ficam registradas aqui. As versões seguem o [versionamento semântico](https://semver.org/lang/pt-BR/): **MAJOR** quebra quem já usa a API, **MINOR** acrescenta recursos e **PATCH** corrige problemas.

## [2.0.1] - 2026-11-03

### Corrigido
- Uma chave inválida ou revogada num `GET` agora recebe **401**. Antes ela era ignorada em silêncio: a requisição seguia como visitante, e quem errou a chave achava que estava no plano USER até levar um 429.
- O rate limit de visitante agora identifica o IP pelo header `CF-Connecting-IP`, que o Cloudflare do Render preenche. Antes ele usava o primeiro valor do `X-Forwarded-For`, que o próprio cliente escreve, e dava para furar o limite mandando um IP falso a cada requisição.

### Alterado
- O Swagger abre direto na v2 e guarda a chave do **Authorize** mesmo depois de recarregar a página.

### Adicionado
- Arquivo `LICENSE` (MIT) na raiz do repositório e este `CHANGELOG.md`.

## [2.0.0] - 2026-11-03

### Quebra compatibilidade
- `POST`, `PUT` e `DELETE` passam a exigir uma chave de API no header `X-API-Key`. A leitura continua livre.
- Todos os erros passam a seguir o formato ProblemDetail (RFC 9457), com `title`, `status`, `detail` e `instance`.

### Adicionado
- Chaves de API com níveis `USER` e `ADMIN`, gerenciadas em `/api-keys`. O banco guarda só o hash SHA-256.
- Planos de rate limit: visitante 20, `USER` 100 e `ADMIN` 1000 requisições por minuto, com `429` e `Retry-After`.
- Idempotência nos `POST` com o header `Idempotency-Key`.
- Versionamento pelo header `API-Version`. A v2 dos pilotos traz a equipe resumida e a idade calculada.
- CORS só para as origens configuradas.
- Camada de serviço entre os controllers e os repositórios.
- Páginas de termos de serviço (`/terms.html`) e licença (`/license.txt`), README e coleção do Postman.

### Obsoleto
- A v1 está obsoleta e tem desligamento previsto para 1º de março de 2027. As respostas dela trazem os headers `Deprecation` e `Sunset`.

### Segurança
- As chaves não ficam mais no código: vêm das variáveis de ambiente ou do arquivo `.env`, e sem elas a API não sobe.

## [1.1.0] - 2026-10-20

### Adicionado
- `Dockerfile` e deploy no Render, com os links e o Swagger saindo em `https` atrás do proxy.

## [1.0.0] - 2026-10-06

### Adicionado
- CRUD de equipes, chefes de equipe, pilotos, patrocinadores e resultados de corrida, com paginação, ordenação e HATEOAS (HAL).
- Classificação de pilotos e de construtores calculada a partir dos resultados.
- Validação dos dados, buscas por filtro, documentação no Swagger e banco H2 em memória com dados iniciais.
