# Architecture

Ultima atualizacao: `2026-03-24`

## Visao geral
- Sistema atual: um unico backend Spring Boot e um unico frontend React.
- Estilo arquitetural do backend: monolito modular.
- Regra central: preservar 1 deploy e 1 aplicacao, preparando a base para eventual extracao futura sem virar microservicos agora.
- Baseline atual: a arquitetura segue modular, mas a modelagem de acesso deixou de tratar `user` como centro de tenant e papel.

## Stack oficial
- Backend: Java 21, Spring Boot 4, Maven, Spring Security, Spring Data JPA, Flyway.
- Banco principal: PostgreSQL 17.6 no RDS.
- Testes: H2 em modo compatibilidade PostgreSQL, Spring Boot Test, Spring Security Test.
- Frontend: React, JavaScript, Vite, React Router, react-oidc-context, React Query, zod.
- Auth: Amazon Cognito + JWT; o Cognito continua sendo a fonte de autenticacao, enquanto a aplicacao resolve o contexto de acesso.
- AWS: ECS/Fargate, ALB, ECR, RDS, Secrets Manager, Lambda de Pre Token Generation.

## Modelo SaaS atual
### Identidade global
- `User` representa a identidade global da pessoa.
- Campos globais continuam em `app_user`: `id`, `identity_subject`, `identity_username`, `email`, `display_name`, `status`, `created_at`.
- Campos legados `role`, `tenant_id` e `tenant_type` ainda existem apenas para compatibilidade incremental.

### Contexto de acesso
- `Membership` e o novo vinculo contextual de acesso.
- Cada `user_membership` responde por `tenant`, `organization`, `market`, status e papel(is) ativos.
- O contexto autenticado agora suporta `membershipId`, `activeTenantId`, `activeOrganizationId`, `activeMarketId`, `roles` e `permissions`.
- A aplicacao resolve esse contexto em `platform.access.AccessContextService`.
- A selecao explicita do contexto pode acontecer por troca do membership default ou por header request-scoped `X-Access-Context`.

### Fronteira SaaS
- `tenant` e a fronteira SaaS explicita.
- `organization` pertence a um `tenant` e pode opcionalmente apontar para um `market`.
- `tenant_market` introduz a dimensao regional/comercial sem acoplar isso ao `user`.
- `organization` continua suportando hierarquia e visibilidade por subarvore.

### Autorizacao
- `app_role`, `app_permission`, `membership_role` e `role_permission` preparam a autorizacao contextual.
- O Cognito nao e usado como modelo completo de autorizacao; JWT autentica, a aplicacao autoriza.
- O contexto ativo pode ser resolvido por subject, username, email e hint de contexto.

## Monolito modular atual
### `app`
- Bootstrap global, `ProgramManagementSystemApplication`, config global e exception handling.

### `platform.auth`
- Configuracao de seguranca, conversao de JWT, filtros e `/api/auth/*` / `/public/auth/*`.

### `platform.authorization`
- Matriz de autorizacao, contexto, decisao e `/api/authz/check`.

### `platform.access`
- Tenant explicito, market, membership, papeis contextuais, permissoes e resolucao do contexto autenticado.
- Agora tambem abriga a administracao de memberships e markets via `/api/access/*`.

### `platform.audit`
- Auditoria persistente e correlacao de requests.
- `RequestCorrelationFilter` propaga `X-Correlation-Id` e apoia rastreio do fluxo de troca de contexto.

### `platform.tenant`
- Organizacoes, hierarquia por subarvore, fronteira organizacional e purge operacional.

### `platform.users`
- Administracao de usuarios, sincronizacao com identidade e compatibilidade incremental com o modelo legado.
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
- `OrganizationBoundaryResolver` existe para leituras leves de tenant/market/tipo da organizacao sem puxar servicos de diretorio mais pesados.
- `OrganizationBootstrapPort` permite ao bootstrap semear organizacoes sem acoplar no `OrganizationDirectoryService`.
- `platform.tenant` consulta usuarios por `TenantUserQueryPort` e `TenantUserPurgePort`.
- `platform.access` consulta usuarios via `UserRepository`, mas nao depende do diretorio completo de tenant para resolver o contexto.
- `modules.projectmanagement` consulta tenant e purge/reset por portas explicitas, sem depender diretamente da infraestrutura concreta.
- `PortfolioResetPort` e `AccessContextResetService` permitem resetar dados funcionais e contextuais durante bootstrap/testes.

## Runtime atual de seguranca
- O backend opera como OAuth2 Resource Server stateless.
- `AuthenticatedUser` agora carrega identidade global e contexto ativo de membership.
- `AuthenticatedUserMapper` tenta resolver membership real primeiro e cai para claims legadas apenas como fallback.
- `GET /api/auth/me` ja expone o contexto ativo, sem remover os campos legados de compatibilidade.
- `AuthenticationLoggingFilter`, `JsonAuthenticationEntryPoint` e `JsonAccessDeniedHandler` registram `correlationId`, presenca de `X-Access-Context` e falhas de autorizacao apos troca de contexto.

## Observabilidade e cache
- Context switch agora e rastreavel ponta a ponta por `correlationId`, `membershipId` anterior/novo e `makeDefault`.
- O frontend centraliza logs estruturados em `src/lib/observability.js` para requests com e sem `X-Access-Context`.
- A UI deixou de usar invalidador amplo no switch de contexto e passou a invalidar seletivamente dominios de `current-user`, `access`, `users`, `portfolio` e `organizations`.

## Compatibilidade incremental
- `app_user.role`, `app_user.tenant_id` e `app_user.tenant_type` seguem persistidos temporariamente.
- `JpaUserRepository` faz dual-write para `user_membership` e `membership_role` ao salvar usuarios legados.
- Claims antigas de tenant no Cognito ainda sao lidas como hint de contexto, nao mais como unica fonte de verdade.
- Fluxos de usuarios, portfolio e bootstrap seguem funcionando sem exigir migracao abrupta do frontend.

## Referencias relacionadas
- [Resumo compartilhado](./PROJECT_CONTEXT.md)
- [Regras de negocio](./BUSINESS_RULES.md)
- [Decisoes](./DECISIONS.md)
- [Gaps abertos](./OPEN_GAPS.md)
- [Hierarquia organizacional](../organization-hierarchy.md)
