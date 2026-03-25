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
- Workspace administrativo de markets em `/workspace/markets`.
- Workspace de portfolio em `/workspace` e detalhe em `/workspace/programs/:programId`.
- Login proprio na home do PMS via `POST /public/auth/login`.
- Primeiro acesso em `/auth/first-access` para `NEW_PASSWORD_REQUIRED`.
- Redefinicao por codigo em `/auth/reset-password` para `PASSWORD_RESET_REQUIRED` e esquecimento de senha.
- Restore de sessao em `sessionStorage` com renovacao via `POST /public/auth/refresh`.
- Logout autenticado via `POST /api/auth/logout`.
- Banner autenticado de verificacao de email no `AppShell`, usando `send-code` + `confirm`.
- Seletor de contexto ativo no `AppShell`, consumindo `POST /api/access/context/activate`.
- Persistencia local do membership selecionado e envio automatico de `X-Access-Context` para operar a sessao no contexto escolhido.
- Gestao membership-first em `/workspace/users`, consumindo `GET/POST/PUT/DELETE /api/access/users/{userId}/memberships`.
- Gestao de `tenant_market` em `/workspace/markets`, consumindo `GET/POST/PUT/DELETE /api/access/tenants/{tenantId}/markets`.
- Diretorios e acoes principais de `users`, `organizations` e `portfolio`.

## Backend pronto, frontend ainda nao adotou completamente
- Tornar mais evidente na UX quando a sessao esta operando em modo request-scoped versus default persistido.
- Expandir a trilha membership-first para substituir gradualmente a dependencia do bloco legado de create/edit em `/api/users`.
- Adicionar refinamentos de busca, filtros e volume para markets e memberships.

## Divergencias ou pontos sensiveis atuais
- O contrato legado de `users` ainda e plano em `organizationId` e `role`, enquanto o backend ja suporta multiplos memberships.
- O frontend agora explora a troca explicita de contexto ativo, mas o legado de `/api/users` continua visivel por compatibilidade.
- Claims legadas no token ainda existem por compatibilidade, mas o backend privilegia o contexto resolvido localmente.
- O `access_token` continua sendo a trilha principal; o fallback de `id_token` ainda precisa ser removido com seguranca.
- A visualizacao de organizacoes ainda e majoritariamente em lista enriquecida; a arvore visual completa continua pendente.

## Pendencias de integracao
- Homologar ponta a ponta a trilha nova de memberships no workspace administrativo.
- Homologar ponta a ponta o seletor de contexto ativo com troca entre tenant, organization e market.
- Homologar ponta a ponta a superficie administrativa de markets por tenant.
- Refinar a sinalizacao visual do bloco legado de `/api/users` como compatibilidade.
- Homologar ponta a ponta a selecao de contexto entre tenants, organizacoes e markets.
- Continuar a homologacao do fluxo de auth proprio com Cognito real.
- Aplicar bucket + policy IAM no runtime AWS para virar documentos de `stub` para `S3` real.

## Roteiro manual sugerido agora
- Criar ou localizar um usuario com mais de um membership.
- Chamar `GET /api/access/users/{userId}/memberships` e validar os contextos retornados.
- Ativar um membership com `POST /api/access/context/activate`.
- Chamar `GET /api/auth/me` com e sem `X-Access-Context` e comparar o contexto resolvido.
- Criar e editar um market com `/api/access/tenants/{tenantId}/markets`.
- Validar no `AppShell` a alternancia entre `Aplicar nesta sessao`, `Salvar como padrao` e `Voltar ao default`.
- Validar que o bloco legado de `/api/users` continua operacional sem quebra.
