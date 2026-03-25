# Project Context

Ultima atualizacao: `2026-03-24`

## Objetivo do produto
- Construir um SaaS B2B de gerenciamento de programas e projetos para a cadeia automotiva.
- Atender montadoras, Tier 1 e Tier 2 com colaboracao multiempresa, rastreabilidade e segregacao por tenant.

## Escopo atual
- Backend unico em Spring Boot 4 + Java 21 + Maven.
- Frontend separado em React + Vite, no workspace local `C:\Users\vande\Oryzem\PMS Frontend`.
- Autenticacao via Amazon Cognito; o backend valida JWTs e resolve autorizacao contextual internamente.
- Persistencia principal em PostgreSQL no RDS, com Flyway e JPA.
- Runtime de dev em AWS com ECS/Fargate, ALB, ECR e Secrets Manager.

## Modulos principais
- `platform.auth`: Cognito, JWT, endpoints publicos de login e `/api/auth/*`.
- `platform.authorization`: matriz de autorizacao e diagnostico em `/api/authz/check`.
- `platform.access`: tenant, market, membership, permissoes contextuais e `/api/access/*`.
- `platform.tenant`: organizacoes, hierarquia, subtree visibility e purge operacional.
- `platform.users`: administracao de usuarios e reconciliacao com identidade.
- `platform.documents`: storage e presigned URLs.
- `modules.projectmanagement`: portfolio e dominio principal.
- `modules.operations`: operacoes administrativas legadas.
- `modules.reports`: relatorios e exportacao.

## Modelo SaaS atual
- `User` representa identidade global.
- `Membership` representa o contexto de acesso.
- `Tenant` representa a fronteira SaaS.
- `Organization` representa a estrutura interna de negocio dentro do tenant.
- `Market` representa a dimensao regional/comercial/operacional.
- `Role` e `Permission` passaram a ser resolvidos no contexto do membership.

## Estado atual resumido
- O backend ja possui `tenant`, `tenant_market`, `user_membership`, `membership_role`, `app_permission` e `role_permission` como estruturas reais.
- `app_user.role`, `app_user.tenant_id` e `app_user.tenant_type` continuam apenas como compatibilidade temporaria.
- `organization` agora pertence a um `tenant` explicito e pode opcionalmente apontar para um `market`.
- `AuthenticatedUser` passou a suportar `membershipId`, `activeTenantId`, `activeOrganizationId`, `activeMarketId`, `roles` e `permissions`.
- O backend ja expoe:
- `GET/POST/PUT/DELETE /api/access/users/{userId}/memberships`
- `POST /api/access/context/activate`
- `GET /api/access/tenants`
- `GET/POST/PUT/DELETE /api/access/tenants/{tenantId}/markets`
- `GET /api/auth/me` agora reflete contexto ativo real, expoe `userId` e aceita `X-Access-Context` para troca por request.
- `GET /api/auth/me` tambem expone `activeTenantName`, `activeOrganizationName` e `activeMarketName` para labels confiaveis na UI.
- O fluxo atual de `users` continua funcional sem quebra abrupta, com sincronizacao incremental entre modelo legado e membership default.
- A hierarquia externa por customer/subarvore continua sendo a base de visibilidade operacional do portfolio.
- O backend continua entregando os modulos de organizacoes, portfolio, users, operations e reports.
- O frontend ja consome login proprio via backend, verificacao explicita de email e os fluxos principais de portfolio e users.
- O frontend agora tambem consome a trilha nova de acesso contextual:
- seletor de contexto ativo no `AppShell`
- administracao de memberships no workspace de users
- listagem de tenants visiveis via `/api/access/tenants` para abastecer markets e memberships
- superficie administrativa de markets em `/workspace/markets`
- uso automatico de `X-Access-Context` para operar a sessao no membership selecionado
- logs estruturados e `correlationId` para rastrear context switch e falhas de autorizacao relacionadas
- invalidação seletiva de cache por dominio apos context switch

## Fluxos principais
- `Tenant -> Organization -> Program -> Project -> Product -> Item -> Deliverable -> Document`
- `User -> Membership -> Tenant / Organization / Market -> Role -> Permission`
- `Organization -> primeiro ADMIN -> onboarding de usuarios`
- `SUPPORT INTERNAL -> purge-subtree` para saneamento operacional controlado

## Compatibilidade em curso
- O contrato administrativo de usuarios ainda usa `organizationId` e `role` planos.
- A aplicacao faz dual-read e dual-write para manter o membership default alinhado ao user legado.
- Claims legadas de tenant no JWT continuam aceitas como fallback quando nao ha contexto resolvido localmente.
- O Cognito continua sendo autenticacao; a aplicacao continua sendo a fonte de autorizacao contextual.

## Proximos passos resumidos
- Migrar o contrato principal de `users` para expor memberships como recurso de primeira classe.
- Refinar a UX da trilha nova de memberships e reduzir dependencia visual do bloco legado de users.
- Evoluir a superficie de `tenant_market` com filtros, busca e naming real de negocio, agora usando `GET /api/access/tenants` como fonte primaria de selecao de tenant.
- Reduzir gradualmente a dependencia funcional de `app_user.role` e `app_user.tenant_id`.
- Executar o checklist de homologacao multi-membership/multi-market e o plano de rollback funcional documentado.
- Continuar a homologacao do fluxo de auth proprio e da operacao AWS para documentos em `S3` real.

## Navegacao
- [Regras de negocio vigentes](./BUSINESS_RULES.md)
- [Contrato de API vigente](./API_CONTRACT.md)
- [Alinhamento frontend/backend](./FRONTEND_BACKEND_ALIGNMENT.md)
- [Arquitetura atual](./ARCHITECTURE.md)
- [Decisoes ativas e temporarias](./DECISIONS.md)
- [Gaps reais ainda abertos](./OPEN_GAPS.md)
- [Checklist de homologacao e rollback](./HOMOLOGATION_CHECKLIST.md)
- [Historico resumido](./CHANGELOG.md)
