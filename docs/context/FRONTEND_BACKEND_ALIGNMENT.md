# Frontend Backend Alignment

Ultima atualizacao: `2026-03-22`

## Backend ja entrega
- `/api/auth/me`, `/api/authz/check`, `/api/users`, `/api/operations`, `/api/reports` e `/api/portfolio` protegidos por JWT do Cognito.
- Contrato de usuarios orientado a `organizationId` e `organizationName`.
- Hierarquia de organizacoes por customer/subarvore.
- Portfolio agregado com milestones, projetos, produtos, itens, entregaveis, documentos e open issues.
- Purge administrativo de usuario e purge de subtree para saneamento operacional controlado.
- Erros `404` e `500` com `correlationId`.

## Frontend ja consome
- Workspace tecnico em `/workspace/session`.
- Workspace administrativo de organizacoes em `/workspace/organizations`.
- Workspace de usuarios em `/workspace/users`.
- Workspace de portfolio em `/workspace` e detalhe em `/workspace/programs/:programId`.
- Endpoints consumidos no frontend:
- `GET /public/auth/config`
- `GET /api/auth/me`
- `GET /api/authz/check`
- `GET/POST/PUT/DELETE /api/users`
- `POST /api/users/{userId}/resend-invite`
- `POST /api/users/{userId}/reset-access`
- `POST /api/users/{userId}/purge`
- `GET/POST/PUT/DELETE /api/portfolio/organizations`
- `GET /api/portfolio/organizations/{organizationId}`
- `POST /api/portfolio/organizations/{organizationId}/purge-subtree`
- `GET/POST /api/portfolio/milestone-templates`
- `GET/POST /api/portfolio/programs`
- `GET /api/portfolio/programs/{programId}`
- `POST /api/portfolio/programs/{programId}/projects`
- `POST /api/portfolio/projects/{projectId}/products`
- `POST /api/portfolio/products/{productId}/items`
- `POST /api/portfolio/items/{itemId}/deliverables`
- `POST /api/portfolio/programs/{programId}/open-issues`
- fluxo de documentos via `upload-url`, `complete`, `download-url` e `delete`

## O que ainda esta stubado ou provisiorio
- Provider de documentos em dev continua em `stub`, inclusive no runtime AWS atual.
- Frontend ainda tem fallback controlado para `id_token` quando o backend responde `403` ao `access_token`.
- A visualizacao de organizacoes ainda e majoritariamente em lista enriquecida; a arvore visual completa ainda nao foi implementada.

## Divergencias ou pontos sensiveis atuais
- Logout do frontend ja foi implementado com Hosted UI, mas segue pendente de homologacao completa em uso real.
- O `access_token` ja carrega claims de tenant; ainda falta decidir quando o fallback de `id_token` pode ser removido.
- O frontend ja recebe metadados hierarquicos completos de organizacao, mas a UX ainda nao representa toda a subarvore de forma nativa.
- `/api/auth/me` hoje e suficiente para sessao e roles, mas a eventual necessidade de enriquecimento adicional ainda esta aberta.
- Persistencia e autorizacao do backend ja respeitam subarvore; a UX de `SUPPORT INTERNAL` ainda precisa de validacao mais ampla em cenarios reais.

## Pendencias de integracao
- Homologar logout completo com Hosted UI e `logout_uri` real.
- Validar visualmente o portfolio proprio vs. herdado da subarvore.
- Confirmar em uso real o fluxo de `SUPPORT INTERNAL` para `resend-invite`, `reset-access` e `purge`.
- Validar o primeiro login do `ADMIN INTERNAL` bootstrapado e o redirect final do frontend.
- Revisar se `/api/auth/me` precisa expor mais contexto para a UI.
- Decidir quando remover o fallback de `id_token`.
- Fechar a passagem de documentos do modo `stub` para S3 real.
