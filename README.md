# Formula 1 API

API RESTful em Spring Boot que gerencia equipes, chefes de equipe, pilotos, patrocinadores e resultados de corrida da Fórmula 1. A partir dos resultados, ela calcula as tabelas de classificação de pilotos e de construtores.

Projeto final da disciplina de Desenvolvimento de APIs com Spring Boot (TSI · Senac).

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 21 + Spring Boot 4.1.1 | Base da aplicação |
| Maven (com wrapper `mvnw`) | Build e dependências |
| H2 (em memória) + Spring Data JPA | Banco de dados e persistência |
| Bean Validation | Validação dos dados de entrada |
| Spring HATEOAS | Links de navegação nas respostas (HAL) |
| Springdoc OpenAPI | Documentação Swagger |
| Bucket4j | Rate limiting |
| Docker | Deploy no Render |

## Como rodar

Pré-requisito: JDK 21 ou superior. Não é preciso instalar o Maven.

As chaves de API não ficam no código. Antes de rodar, copie o `.env.example` para `.env` e troque os valores. O `.env` fica fora do git, e sem ele a API não sobe.

```bash
# Windows
copy .env.example .env
.\mvnw.cmd spring-boot:run

# Linux / Mac
cp .env.example .env
./mvnw spring-boot:run
```

A API sobe em **http://localhost:8080**.

| Endereço | O que é |
|---|---|
| http://localhost:8080/swagger-ui.html | Documentação interativa (Swagger) |
| http://localhost:8080/ | Raiz da API, com links para todos os recursos |
| http://localhost:8080/terms.html | Termos de serviço (página pública) |
| http://localhost:8080/license.txt | Licença MIT (pública) |
| http://localhost:8080/h2-console | Console do banco, só abre no próprio computador. JDBC URL `jdbc:h2:mem:formula1db`, usuário `sa`, senha vazia |

O banco é em memória: a cada execução ele recomeça com os dados iniciais (4 equipes, 4 chefes, 8 pilotos, 5 patrocinadores e 16 resultados ilustrativos).

### Com Docker

```bash
docker build -t formula1api .
docker run -p 8080:8080 --env-file .env formula1api
```

### Testes

```bash
.\mvnw.cmd test
```

Os testes sobem a aplicação inteira e verificam a chave de API, a idempotência, o rate limit, o versionamento, o CORS e as mensagens de validação.

## Modelo de dados

| Entidade | Relacionamentos |
|---|---|
| `Team` (equipe) | 1:N com Driver · 1:1 com TeamPrincipal · N:N com Sponsor |
| `TeamPrincipal` (chefe de equipe) | **One-to-One** com Team |
| `Driver` (piloto) | **Many-to-One** com Team (o inverso é o **One-to-Many**) · 1:N com RaceResult |
| `Sponsor` (patrocinador) | **Many-to-Many** com Team |
| `RaceResult` (resultado) | N:1 com Driver · enum `ResultStatus` (FINISHED, DNF, DSQ) |
| `ApiKey` (chave de API) | enum `ApiKeyRole` (USER, ADMIN) |

A pontuação não é guardada no piloto: é calculada somando os pontos dos resultados (`GET /drivers/standings` e `GET /teams/standings`).

## Arquitetura

O projeto separa as camadas **Controller → Service → Repository**:

- **Controller:** recebe o HTTP, valida a entrada (`@Valid`), documenta o endpoint (Swagger) e monta a resposta com HATEOAS.
- **Service:** regras de negócio (resolver referências por id, regras de exclusão, classificação).
- **Repository:** acesso ao banco com Spring Data JPA, incluindo as consultas personalizadas.

Os recursos transversais ficam em filtros que rodam antes dos controllers, nesta ordem:

```
CorsFilter (0) → RateLimitFilter (1) → ApiKeyFilter (2) → IdempotencyFilter (3) → Controller → Service → Repository
```

```
src/main/java/com/senac/tsi/Formula1Api/
├── <Entidade>.java / <Entidade>Repository / <Entidade>Service / <Entidade>Controller / <Entidade>ModelAssembler
├── ApiKeyFilter, RateLimitFilter, IdempotencyFilter, CorsConfig, ApiVersionConfig   ← recursos da parte 2
├── ApiExceptionAdvice, ProblemResponse, ConflictException, *NotFoundException      ← tratamento de erros
├── OpenApi, HomeController, LoadDatabase
└── v2/   ← versão 2 dos pilotos (DriverV2Controller e DTOs)
```

## Endpoints

Cada entidade tem CRUD completo, listagem paginada (`page`, `size`, `sort`) e consultas personalizadas. A documentação completa, com exemplos, está no Swagger.

| Recurso | CRUD | Consultas personalizadas |
|---|---|---|
| Equipes | `/teams`, `/teams/{id}` | `/teams/search?country=` · `/teams/sponsor/{sponsorId}` · `/teams/standings` |
| Chefes de equipe | `/principals`, `/principals/{id}` | `/principals/search?nationality=` · `/principals/team/{teamId}` |
| Pilotos | `/drivers`, `/drivers/{id}` | `/drivers/search?name=` · `/drivers/team/{teamId}` · `/drivers/standings` |
| Patrocinadores | `/sponsors`, `/sponsors/{id}` | `/sponsors/search?industry=` |
| Resultados | `/results`, `/results/{id}` | `/results/status/{status}` · `/results/search?race=` · `/results/driver/{driverId}` |
| Chaves de API | `/api-keys`, `/api-keys/{id}` | `/api-keys/role/{role}` · `/api-keys/me` |

## Recursos avançados (parte 2)

### Autenticação com chave de API (`X-API-Key`)

| Operação | Exige |
|---|---|
| `GET` nos dados | Nada (leitura pública). A chave é opcional, mas se for enviada precisa ser válida |
| `POST`, `PUT`, `DELETE` nos dados | Chave `USER` ou `ADMIN` |
| Gerenciar chaves em `/api-keys` | Chave `ADMIN` (o `/api-keys/me` aceita qualquer chave) |

Duas chaves são criadas quando a aplicação sobe, com os valores das variáveis de ambiente (no seu computador, do `.env`):

| Variável | Nível |
|---|---|
| `ADMIN_API_KEY` | ADMIN |
| `USER_API_KEY` | USER |

Uma chave ADMIN gera chaves novas com `POST /api-keys` (`{ "name": "Front-end", "role": "USER" }`). A chave completa aparece **só na resposta da criação**. O banco guarda apenas o hash SHA-256 dela. `PUT /api-keys/{id}` com `"active": false` desativa a chave, e `DELETE` revoga. Sem chave ou com chave inválida, a resposta é **401**; com chave USER em `/api-keys`, **403**. Isso vale também nos `GET`: uma chave errada não é ignorada, para ninguém achar que está no plano USER sem estar.

No Swagger, clique em **Authorize** e cole a chave.

### Níveis de acesso

| Quem | O que pode |
|---|---|
| Visitante (sem chave) | Ler os dados (`GET`), até 20 requisições por minuto |
| Chave `USER` | Ler e alterar os dados (`POST`, `PUT`, `DELETE`), até 100 por minuto |
| Chave `ADMIN` | Tudo isso e também gerenciar as chaves (`/api-keys`), até 1000 por minuto |

### Idempotência (`Idempotency-Key`)

Todo `POST` aceita o header `Idempotency-Key` (por exemplo, um UUID). Repetir a mesma chave com o mesmo corpo **não cria outro registro**: a API devolve a resposta original, com o header `Idempotent-Replayed: true`. A mesma chave com outro corpo dá **422**. As chaves valem por 24 horas e são separadas por cliente (chave de API) e por versão.

### Rate limiting

O limite depende do **plano** de quem chama:

| Plano | Limite | Conta por | Variável para trocar |
|---|---|---|---|
| Visitante (sem chave) | 20 requisições por minuto | IP | `RATE_LIMIT_PER_MINUTE` |
| Chave `USER` | 100 requisições por minuto | Chave | `RATE_LIMIT_USER_PER_MINUTE` |
| Chave `ADMIN` | 1000 requisições por minuto | Chave | `RATE_LIMIT_ADMIN_PER_MINUTE` |

O limite da chave vale em qualquer IP. Uma chave inválida recebe 401, e a tentativa conta no limite de visitante. No Render, o IP do visitante vem do header `CF-Connecting-IP`, que o Cloudflare preenche e o cliente não consegue falsificar. Toda resposta traz `X-Rate-Limit-Limit`, com o limite do seu plano, e `X-Rate-Limit-Remaining`. Passou do limite, a resposta é **429 Too Many Requests**, com o header `Retry-After` dizendo em quantos segundos tentar de novo. O Swagger e o console do H2 não contam no limite.

### CORS

Só as origens configuradas podem chamar a API pelo navegador. O padrão é `http://localhost:3000`, `http://localhost:5173` e `http://127.0.0.1:5500`, configurável em `CORS_ALLOWED_ORIGINS`, separado por vírgula. A configuração libera os métodos `GET`, `POST`, `PUT`, `DELETE` e `OPTIONS`, aceita os headers da API e expõe os headers de resposta (`Location`, `Retry-After`, `Idempotent-Replayed`...). Uma origem não permitida recebe **403** no preflight.

### Versionamento (`API-Version`)

Usa o versionamento nativo do Spring 7. A versão vai no header `API-Version` (ou em `?api-version=`). Sem versão, a requisição vai para a v1.

| | v1 (padrão) | v2 (`API-Version: 2`) |
|---|---|---|
| Equipe no corpo do POST/PUT | `"team": { "id": 1 }` | `"teamId": 1` |
| Equipe na resposta | Completa, com patrocinadores | Resumida: `{ "id", "name" }` |
| Idade do piloto | Não tem | Calculada (`age`) |
| Formato | HAL com `_links` | JSON simples (DTO) |

A v2 vale para listar, buscar, criar e alterar pilotos. Os outros endpoints servem as duas versões. Toda resposta v1 dos pilotos traz os headers `Deprecation` e `Sunset`. Uma versão inexistente (ex.: `3`) dá **400**. O Swagger tem um documento por versão (seletor no topo).

## Validações e erros

Todos os dados de entrada são validados com Bean Validation. Os erros são tratados num lugar só (`ApiExceptionAdvice`, um `@RestControllerAdvice`) e voltam sempre no formato **ProblemDetail** (RFC 9457):

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/drivers",
  "errors": { "number": "deve ser menor que ou igual à 99" }
}
```

| Código | Quando |
|---|---|
| 200 / 201 / 204 | Sucesso (o 201 traz o header `Location`) |
| 400 | Validação, JSON malformado, tipo errado na URL ou versão inexistente |
| 401 | Sem `X-API-Key` ou chave inválida/revogada |
| 403 | Chave USER tentando gerenciar chaves, ou origem bloqueada pelo CORS |
| 404 | Registro não encontrado |
| 409 | Valor repetido ou exclusão de algo ainda em uso (ex.: equipe com pilotos) |
| 422 | `Idempotency-Key` reutilizada com outro corpo |
| 429 | Rate limit excedido |

## Coleção do Postman

O arquivo [`postman/Formula1Api.postman_collection.json`](postman/Formula1Api.postman_collection.json) tem 68 requisições, organizadas por recurso, mais uma pasta de demonstrações da parte 2 e uma de erros. Cada requisição tem um teste do status esperado.

1. No Postman: **Import** → selecione o arquivo.
2. Preencha as variáveis da coleção: `userKey` e `adminKey` com as suas chaves (do `.env` ou do Render) e, se for usar o Render, `baseUrl`.
3. Para rodar a coleção inteira no **Runner**, coloque um delay de 3000 ms entre as requisições, ou suba a API com `RATE_LIMIT_PER_MINUTE=1000`. Sem isso, o rate limit de visitante (as leituras sem chave) barra no meio.

## Deploy no Render

O `Dockerfile` faz o build com Maven e roda o `.jar`. No Render, crie um **Web Service** a partir do repositório, com linguagem **Docker** e plano **Free**. Variáveis:

| Variável | Obrigatória | Para quê |
|---|---|---|
| `ADMIN_API_KEY` | Sim | Chave ADMIN, secreta |
| `USER_API_KEY` | Sim | Chave USER, a que você entrega para quem vai usar a API |
| `CORS_ALLOWED_ORIGINS` | Não | Sites que podem chamar a API pelo navegador |

Sem as duas chaves, a API não sobe e o deploy falha. Assim ela nunca roda com uma chave conhecida. Para gerar uma chave, rode `[guid]::NewGuid().ToString("N")` no PowerShell.

O Render define a `PORT` sozinho. No plano gratuito, o serviço dorme depois de 15 minutos sem uso, e o primeiro acesso depois disso leva cerca de 1 minuto.

## Versões e licença

O histórico de mudanças fica no [CHANGELOG.md](CHANGELOG.md). O código está sob a licença MIT, no arquivo [LICENSE](LICENSE).
