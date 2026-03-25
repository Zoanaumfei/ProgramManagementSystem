# API Contract

Ultima atualizacao: `2026-03-24`

## Auth e erros
- Backend protegido por Bearer JWT do Cognito.
- Endpoints publicos atuais:
- `GET /public/ping`
- `GET /public/auth/config`
- `POST /public/auth/login`
- `POST /public/auth/login/new-password`
- `POST /public/auth/password-reset/code`
- `POST /public/auth/password-reset/confirm`
- `POST /public/auth/refresh`
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `401` e retornado sem token valido.
- `403` e retornado quando a sessao autenticada nao possui permissao.
- `409` e retornado para conflitos de negocio tratados.
- `429` e retornado quando o Cognito aplica rate limit tratado pelo backend.
- `404` e `500` retornam JSON com `correlationId`.

## Sessao e autorizacao
### `GET /public/auth/config`
- Uso: bootstrap do frontend.
- Resposta: `provider`, `mode=custom-login-ready`, `issuerUri`, `jwkSetUri`, `appClientId`, `timestamp`.

### `POST /public/auth/login`
- Request: `username`, `password`
- Uso: login programatico do frontend contra Cognito, sem Hosted UI.
- Response:
- `status=AUTHENTICATED` com `accessToken`, `idToken`, `refreshToken`, `expiresIn`, `tokenType`
- `status=NEW_PASSWORD_REQUIRED` com `challengeName=NEW_PASSWORD_REQUIRED` e `session`
- `status=PASSWORD_RESET_REQUIRED` quando o usuario precisa concluir reset por codigo
- `401` para credenciais invalidas

### `POST /public/auth/login/new-password`
- Request: `username`, `session`, `newPassword`
- Uso: concluir o desafio `NEW_PASSWORD_REQUIRED`.
- Response: mesmo payload de autenticacao com `status=AUTHENTICATED`.

### `POST /public/auth/password-reset/code`
- Request: `username`
- Uso: solicitar codigo de reset de senha para login proprio ou self-service.
- Response: `status=CODE_SENT`, `username`, `deliveryMedium`, `destination`.

### `POST /public/auth/password-reset/confirm`
- Request: `username`, `code`, `newPassword`
- Uso: confirmar o codigo de reset recebido por email.
- Response: `status=PASSWORD_RESET_CONFIRMED`, `username`.

### `POST /public/auth/refresh`
- Request: `username`, `refreshToken`
- Uso: renovar sessao no frontend proprio sem Hosted UI.
- Response: mesmo payload de autenticacao com `status=AUTHENTICATED`.

### `GET /api/auth/me`
- Uso: contexto autenticado atual.
- Resposta atual inclui:
- identidade: `subject`, `username`, `email`, `emailVerified`, `emailVerificationRequired`, `tokenUse`
- contexto novo: `membershipId`, `activeTenantId`, `activeOrganizationId`, `activeMarketId`, `roles`, `permissions`
- compatibilidade: `tenantId`, `tenantType`, `tenantIdClaim`, `tenantTypeClaim`, `userStatusClaim`
- diagnostico: `groups`, `scopes`, `authorities`, `timestamp`
- Header opcional: `X-Access-Context`
- Efeito do header: permite pedir um membership/contexto especifico por request sem trocar o default persistido.

## Access
### `GET /api/access/users/{userId}/memberships`
- Uso: listar memberships visiveis de um usuario.
- Response: lista de `MembershipResponse`.

### `POST /api/access/users/{userId}/memberships`
- Uso: criar um membership adicional para um usuario existente.
- Request: `tenantId`, `organizationId`, `marketId`, `status`, `defaultMembership`, `roles[]`.
- Response: `201 Created` com `MembershipResponse`.

### `PUT /api/access/users/{userId}/memberships/{membershipId}`
- Uso: atualizar tenant/contexto/roles/status de um membership existente.
- Request: mesmo shape de criacao.
- Response: `MembershipResponse`.

### `DELETE /api/access/users/{userId}/memberships/{membershipId}`
- Uso: inativar um membership.
- Response: `MembershipResponse`.

### `POST /api/access/context/activate`
- Uso: trocar explicitamente o contexto ativo do proprio usuario.
- Request: `membershipId`, `makeDefault`.
- Response: `ActiveAccessContextResponse`.
- Observacao: com `makeDefault=true`, o membership passa a ser o contexto default resolvido pela aplicacao.

### `GET /api/access/tenants/{tenantId}/markets`
- Uso: listar markets de um tenant.
- Response: lista de `TenantMarketResponse`.

### `POST /api/access/tenants/{tenantId}/markets`
- Uso: criar market explicito em um tenant.
- Request: `code`, `name`, `status`, `currencyCode`, `languageCode`, `timezone`.
- Response: `201 Created` com `TenantMarketResponse`.

### `PUT /api/access/tenants/{tenantId}/markets/{marketId}`
- Uso: atualizar market.
- Response: `TenantMarketResponse`.

### `DELETE /api/access/tenants/{tenantId}/markets/{marketId}`
- Uso: inativar market.
- Response: `TenantMarketResponse`.

### `GET /api/authz/check`
- Uso: diagnostico e validacao da matriz de autorizacao.
- Parametros principais: `module`, `action`, `resourceTenantId`, `resourceTenantType`, `targetRole`, `supportOverride`, `justification`.
- Resposta: `allowed`, `reason`, `restrictions`, `auditRequired`, `maskedViewRequired`, `crossTenant`, `timestamp`.

## Organizacoes
### `GET /api/portfolio/organizations`
- Filtros: `status`, `setupStatus`, `customerOrganizationId`, `parentOrganizationId`, `hierarchyLevel`, `search`.
- Resposta: lista de `OrganizationResponse`.
- Campos principais: `id`, `name`, `code`, `tenantId`, `marketId`, `tenantType`, `parentOrganizationId`, `customerOrganizationId`, `hierarchyLevel`, `childrenCount`, `hasChildren`, `status`, `setupStatus`, `userSummary`, `programSummary`, `canInactivate`, `inactivationBlockedReason`, `createdAt`, `updatedAt`.

### `GET /api/portfolio/organizations/{organizationId}`
- Uso: detalhe administrativo de organizacao.
- Resposta: mesmo shape de `OrganizationResponse`.

### `POST /api/portfolio/organizations`
- Request: `name`, `code`, `parentOrganizationId`, `status`.
- Observacao: `parentOrganizationId=null` cria tenant/organizacao raiz e exige `ADMIN INTERNAL`.
- Response: `OrganizationResponse`.

### `PUT /api/portfolio/organizations/{organizationId}`
- Request: `name`, `code`.
- Response: `OrganizationResponse`.

### `DELETE /api/portfolio/organizations/{organizationId}`
- Sem body.
- Efeito: inativacao logica.
- Response: `OrganizationResponse`.

### `POST /api/portfolio/organizations/{organizationId}/purge-subtree`
- Query params: `supportOverride`, `justification`.
- Response: `organizationId`, `action`, `performedAt`, `status`, `purgedOrganizations`, `purgedPrograms`, `purgedUsers`, `purgedDocuments`.

## Users
### `GET /api/users`
- Filtros: `organizationId`, `tenantId` legado, `supportOverride`, `justification`.
- Response: lista de `UserSummaryResponse`.

### `POST /api/users`
- Request: `displayName`, `email`, `role`, `organizationId`.
- Response: `201 Created` com `UserSummaryResponse`.
- Observacao: o contrato ainda recebe um papel unico e uma organizacao principal; internamente isso sincroniza o membership default durante a transicao.

### `PUT /api/users/{userId}`
- Request: `displayName`, `email`, `role`, `organizationId`.
- Response: `UserSummaryResponse`.

### `DELETE /api/users/{userId}`
- Efeito: inativacao logica.
- Response: `204 No Content`.

### `POST /api/users/{userId}/resend-invite`
### `POST /api/users/{userId}/reset-access`
### `POST /api/users/{userId}/purge`
- Query params sensiveis: `supportOverride`, `justification`.
- Response: `userId`, `action`, `performedAt`, `status`.

### `UserSummaryResponse`
- `id`, `displayName`, `email`, `role`, `organizationId`, `organizationName`, `status`, `createdAt`, `inviteResentAt`, `accessResetAt`

## Portfolio
### Milestone templates
- `GET /api/portfolio/milestone-templates`
- `POST /api/portfolio/milestone-templates`

### Programs
- `GET /api/portfolio/programs`
- Filtro: `ownerOrganizationId`
- `POST /api/portfolio/programs`
- `GET /api/portfolio/programs/{programId}`

### Camadas de criacao
- `POST /api/portfolio/programs/{programId}/projects`
- `POST /api/portfolio/projects/{projectId}/products`
- `POST /api/portfolio/products/{productId}/items`
- `POST /api/portfolio/items/{itemId}/deliverables`
- `POST /api/portfolio/programs/{programId}/open-issues`

### Documentos de entregavel
- `GET /api/portfolio/deliverables/{deliverableId}/documents`
- `POST /api/portfolio/deliverables/{deliverableId}/documents/upload-url`
- `POST /api/portfolio/deliverables/{deliverableId}/documents/{documentId}/complete`
- `POST /api/portfolio/deliverables/{deliverableId}/documents/{documentId}/download-url`
- `DELETE /api/portfolio/deliverables/{deliverableId}/documents/{documentId}`

## Observacao de compatibilidade
- Ainda nao existem endpoints publicos para administrar multiplos memberships por usuario.
- A primeira fase usa `user` legado como fonte de sincronizacao do membership default.
- Claims legadas de tenant seguem aceitas como hint de resolucao de contexto, nao como verdade absoluta.
