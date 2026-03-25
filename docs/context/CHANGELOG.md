# Changelog

Ultima atualizacao: `2026-03-24`

## 2026-03-07
- Base inicial de autenticacao, autorizacao, persistencia e modulos `users`, `operations` e `reports`.

## 2026-03-08 a 2026-03-10
- Consolidacao da operacao AWS com RDS privado, ECS/Fargate, ALB, ECR, Secrets Manager e health checks.

## 2026-03-11
- Fechamento do mapa inicial do dominio principal com raiz em `Programa`.

## 2026-03-13
- `docs/context/PROJECT_CONTEXT.md` passou a ser a memoria operacional compartilhada entre backend e frontend.

## 2026-03-14
- Primeira UI basica do portfolio no frontend consumindo o backend real.
- Ajustes de tratamento de erro e compatibilidade de token no frontend.

## 2026-03-15
- Contrato de `users` migrou para `organizationId` e `organizationName`.
- Jornada integrada `Organizacao -> primeiro ADMIN` entrou no desenho funcional.

## 2026-03-17
- Modulo de `users` ganhou update e inativacao logica.
- `setupStatus` de organizacao e bloqueios de onboarding foram implantados.

## 2026-03-19
- Integracao do modulo de `users` com identidade real no Cognito.
- Lambda de `Pre Token Generation` passou a emitir claims de tenant no `access_token`.
- Workspace de `users` foi implementado no frontend.
- Fluxo de `purge` foi validado no runtime AWS apos ajuste de IAM.

## 2026-03-20
- Hierarquia de organizacoes por customer/subarvore entrou no backend.
- Regras de visibilidade e permissoes de portfolio por papel foram fechadas.
- Frontend foi alinhado ao contrato hierarquico de organizacoes e ao novo recorte de papeis.
- `purge-subtree` entrou no frontend para saneamento operacional restrito.

## 2026-03-21
- Segunda passada do refactor para monolito modular: movimentos fisicos, servicos menores em `projectmanagement` e alinhamento de packages.
- Consolidacao de fronteiras: `OrganizationLookup` movido para `tenant`, tenant/users/reports passaram a conversar por portas explicitas e servicos grandes foram quebrados.
- Testes e pacotes legados foram reorganizados sem mudar o comportamento funcional.

## 2026-03-22
- O contexto compartilhado foi consolidado em `docs/context/PROJECT_CONTEXT.md` como fonte unica.
- `platform.users` foi consolidado como primeiro modulo com camadas internas mais claras: `api`, `application`, `domain` e `infrastructure`.
- DTOs, servicos, repositorios e integracao de identidade de `users` foram movidos fisicamente para pastas coerentes com seus packages.
- O contrato de tenant permaneceu em `platform.tenant.OrganizationLookup`, mantendo `users` desacoplado da implementacao concreta de diretorio organizacional.
- O fluxo de documentos foi preparado para `S3` real sem mudar o contrato HTTP: upload assinado, `complete`, listagem e download assinado.

## 2026-03-23
- O app client do Cognito em dev foi alinhado para `ALLOW_USER_PASSWORD_AUTH`, alem dos flows administrativos ja habilitados.
- Foi identificado e corrigido o binding incorreto do provider de autenticacao publica para o gateway `cognito`.
- Entrou teste de regressao em `PublicAuthenticationConfigTest` para garantir a selecao correta do gateway por provider.

## 2026-03-24
- Entrou a migration `V7__introduce_tenant_membership_and_market.sql`.
- Entrou a migration `V8__allow_multiple_memberships_per_user.sql` para remover a restricao indevida que limitava memberships adicionais.
- `tenant`, `tenant_market`, `user_membership`, `membership_role`, `app_permission` e `role_permission` passaram a existir explicitamente.
- `organization` passou a carregar `tenant_id` explicito e `market_id` opcional sem perder a hierarquia por subarvore.
- `AuthenticatedUser` e `AuthenticatedUserMapper` passaram a suportar `membershipId`, `activeTenantId`, `activeOrganizationId`, `activeMarketId`, `roles` e `permissions`.
- `AccessContextService` passou a resolver o contexto ativo a partir de membership e a sincronizar um membership default a partir do modelo legado.
- Entraram as APIs `/api/access/users/{userId}/memberships`, `/api/access/context/activate` e `/api/access/tenants/{tenantId}/markets`.
- O backend passou a aceitar `X-Access-Context` para trocar o contexto ativo por request sem alterar o token do Cognito.
- Organizacoes raiz agora provisionam `tenant` explicito e o bootstrap/reset passou a limpar tambem as estruturas contextuais novas.
- Fluxos de `users`, `portfolio` e `/api/auth/me` seguem compatíveis com campos legados durante a transicao.
- A pasta `docs` foi realinhada ao modelo `User -> Membership -> Tenant / Organization / Market -> Role -> Permission`, incluindo contexto ativo e gaps atualizados.
