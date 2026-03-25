# Frontend Backend Alignment

Ultima atualizacao: `2026-03-24`

## Backend ja entrega
- `/api/auth/me`, `/api/authz/check`, `/api/users`, `/api/operations`, `/api/reports` e `/api/portfolio` protegidos por JWT do Cognito.
- Endpoints publicos de auth para login proprio: `login`, `new-password`, `password-reset`, `refresh`.
- Contexto autenticado resolvido por membership com `membershipId`, `activeTenantId`, `activeOrganizationId`, `activeMarketId`, `roles` e `permissions`.
- Header opcional `X-Access-Context` para selecionar contexto por request.
- APIs novas de acesso:
- `GET/POST/PUT/DELETE /api/access/users/{userId}/memberships`
- `POST /api/access/context/activate`
- `GET/POST/PUT/DELETE /api/access/tenants/{tenantId}/markets`
- Hierarquia de organizacoes por customer/subarvore.
- Portfolio agregado com milestones, projetos, produtos, itens, entregaveis, documentos e open issues.

## Frontend ja consome
- Workspace tecnico em `/workspace/session`.
- Workspace administrativo de organizacoes em `/workspace/organizations`.
- Workspace de usuarios em `/workspace/users`.
- Workspace de portfolio em `/workspace` e detalhe em `/workspace/programs/:programId`.
- Login proprio na home do PMS via `POST /public/auth/login`.
- Primeiro acesso em `/auth/first-access` para `NEW_PASSWORD_REQUIRED`.
- Redefinicao por codigo em `/auth/reset-password` para `PASSWORD_RESET_REQUIRED` e esquecimento de senha.
- Restore de sessao em `sessionStorage` com renovacao via `POST /public/auth/refresh`.
- Logout autenticado via `POST /api/auth/logout`.
- Banner autenticado de verificacao de email no `AppShell`, usando `send-code` + `confirm`.
- Diretorios e acoes principais de `users`, `organizations` e `portfolio`.

## Backend pronto, frontend ainda nao adotou completamente
- Selecionar contexto ativo do usuario por `POST /api/access/context/activate`.
- Listar e administrar varios memberships por usuario.
- Administrar `tenant_market` de forma explicita.
- Enviar `X-Access-Context` quando a UI operar em contexto nao-default.
- Mostrar no diretorio administrativo a composicao completa `tenant + organization + market + roles` por membership.

## Divergencias ou pontos sensiveis atuais
- O contrato legado de `users` ainda e plano em `organizationId` e `role`, enquanto o backend ja suporta multiplos memberships.
- O frontend ainda nao explora a troca explicita de contexto ativo; hoje tende a operar no membership default.
- Claims legadas no token ainda existem por compatibilidade, mas o backend privilegia o contexto resolvido localmente.
- O `access_token` continua sendo a trilha principal; o fallback de `id_token` ainda precisa ser removido com seguranca.
- A visualizacao de organizacoes ainda e majoritariamente em lista enriquecida; a arvore visual completa continua pendente.

## Pendencias de integracao
- Adotar `/api/access/users/{userId}/memberships` no workspace administrativo.
- Adotar `/api/access/context/activate` no app shell para troca de contexto.
- Adotar `/api/access/tenants/{tenantId}/markets` em uma superficie de configuracao administrativa.
- Refletir em UI o contexto ativo retornado por `/api/auth/me`.
- Homologar ponta a ponta a selecao de contexto entre tenants, organizacoes e markets.
- Continuar a homologacao do fluxo de auth proprio com Cognito real.
- Aplicar bucket + policy IAM no runtime AWS para virar documentos de `stub` para `S3` real.

## Roteiro manual sugerido agora
- Criar ou localizar um usuario com mais de um membership.
- Chamar `GET /api/access/users/{userId}/memberships` e validar os contextos retornados.
- Ativar um membership com `POST /api/access/context/activate`.
- Chamar `GET /api/auth/me` com e sem `X-Access-Context` e comparar o contexto resolvido.
- Criar e editar um market com `/api/access/tenants/{tenantId}/markets`.
- Validar que a UI atual ainda opera no contexto default sem quebra.
