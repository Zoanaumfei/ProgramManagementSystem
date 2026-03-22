# Architecture

Ultima atualizacao: `2026-03-22`

## Visao geral
- Sistema atual: um unico backend Spring Boot e um unico frontend React.
- Estilo arquitetural do backend: monolito modular.
- Regra central: preservar 1 deploy e 1 aplicacao, preparando a base para eventual extracao futura sem virar microservicos agora.
- Baseline atual: a arquitetura modular presente esta aceita como suficiente; as proximas iteracoes devem voltar o foco para produto.

## Stack oficial
- Backend: Java 21, Spring Boot 4, Maven, Spring Security, Spring Data JPA, Flyway.
- Banco principal: PostgreSQL 17.6 no RDS.
- Testes: H2 em modo compatibilidade PostgreSQL, Spring Boot Test, Spring Security Test.
- Frontend: React, JavaScript, Vite, React Router, react-oidc-context, React Query, zod.
- Auth: Amazon Cognito Hosted UI + JWT.
- AWS: ECS/Fargate, ALB, ECR, RDS, Secrets Manager, Lambda de Pre Token Generation.

## Monolito modular atual
### `app`
- Bootstrap global, `ProgramManagementSystemApplication`, config global e exception handling.

### `platform.auth`
- Configuracao de seguranca, conversao de JWT, filtros e `/api/auth/*`.

### `platform.authorization`
- Matriz de autorizacao, contexto, decisao e `/api/authz/check`.

### `platform.audit`
- Auditoria persistente e correlacao de requests.

### `platform.tenant`
- Organizacoes, hierarquia por customer/subarvore, consulta de tenant e purge operacional.

### `platform.users`
- Administracao de usuarios, sincronizacao com identidade e reconciliacao de sessao.
- Estrutura interna atual: `api`, `application`, `domain` e `infrastructure`.

### `platform.documents`
- Storage de documentos, presigned URLs e provider configuravel.

### `modules.projectmanagement`
- Portfolio e dominio principal, com subresponsabilidades internas de `program`, `project`, `execution`, `governance`, `templates`, `access` e `lookup`.

### `modules.operations`
- Operacoes administrativas legadas.

### `modules.reports`
- Relatorios e exportacao.

## Fronteiras importantes entre modulos
- `OrganizationLookup` pertence a `platform.tenant`.
- `OrganizationBootstrapPort` permite ao bootstrap semear organizacoes sem acoplar no `OrganizationDirectoryService`.
- `platform.tenant` consulta usuarios por `TenantUserQueryPort` e `TenantUserPurgePort`.
- `modules.reports` le usuarios e operacoes por `ReportUserQueryPort` e `ReportOperationQueryPort`.
- `modules.projectmanagement` consulta tenant e purge/reset por portas explicitas, sem depender diretamente da infraestrutura de `tenant`.
- `PortfolioResetPort` permite ao bootstrap limpar `projectmanagement` sem depender do servico concreto.
- `platform.documents` expoe `PortfolioDocumentStorageGateway` para o portfolio sem vazar JPA de outro modulo.

## Runtime atual de dev AWS
- Regiao: `sa-east-1`
- Cognito user pool: `sa-east-1_aA4I3tEmF`
- Cognito app client: `rv7hk9nkugspb3i4p269sv828`
- Hosted UI: `https://sa-east-1aa4i3temf.auth.sa-east-1.amazoncognito.com`
- RDS: `program-management-system-db`
- ECS cluster: `program-management-system-cluster`
- ECS service: `program-management-system-service`
- Task definition validada: `program-management-system:21`
- ALB DNS atual: `program-management-system-alb-1082436660.sa-east-1.elb.amazonaws.com`
- Secret do banco: `program-management-system/rds/master`
- Lambda de token: `program-management-system-cognito-pre-token`

## Observacoes tecnicas vigentes
- O backend opera como OAuth2 Resource Server stateless; nao existe login por senha local.
- A autorizacao final e feita na aplicacao, nao apenas nas claims.
- O provider de documentos continua em `stub` no runtime atual; S3 real ainda nao foi fechado.
- `ResourceNotFoundException` agora mora em `platform.shared`, e nao mais em `app`.
- `PortfolioOrganizationService` e `UserManagementService` ja foram quebrados em servicos menores por responsabilidade.
- `platform.users` ja nao depende diretamente de implementacao de tenant; a consulta de organizacao segue via `platform.tenant.OrganizationLookup`.
- `app.bootstrap.BootstrapDataService` ainda usa `UserRepository` e `OperationRepository`, mas isso foi aceito como wiring de bootstrap por consumir contratos dos modulos, nao implementacoes concretas.
- Guardrails com ArchUnit agora protegem fronteiras basicas e evitam regressao estrutural obvia.

## Referencias relacionadas
- [Resumo compartilhado](./PROJECT_CONTEXT.md)
- [Regras de negocio](./BUSINESS_RULES.md)
- [Decisoes](./DECISIONS.md)
- [Gaps abertos](./OPEN_GAPS.md)
- [Detalhe do refactor modular](../../MODULAR_MONOLITH_REFACTOR.md)
