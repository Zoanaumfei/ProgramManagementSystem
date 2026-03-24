# API Contract

Ultima atualizacao: `2026-03-22`

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
- Corpo padrao de erro tratado:
- `timestamp`
- `status`
- `error`
- `message` para erros tratados pelo `ApiExceptionHandler`
- `path`
- `correlationId` quando disponivel
- `401` e `403` retornam JSON curto com `timestamp`, `status`, `error` e `path`.

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
- Uso: contexto da sessao autenticada.
- Resposta atual inclui:
- `subject`, `username`, `email`, `emailVerified`, `emailVerificationRequired`, `tokenUse`
- `tenantId`, `tenantType`
- `tenantIdClaim`, `tenantTypeClaim`, `userStatusClaim`
- `roles`, `groups`, `scopes`, `authorities`, `timestamp`

### `GET /api/auth/email-verification`
- Uso: ler o estado atual da verificacao do email do usuario autenticado.
- Resposta: `email`, `emailVerified`, `emailVerificationRequired`, `status`, `attributeName`, `deliveryMedium`, `destination`.

### `POST /api/auth/email-verification/code`
- Uso: solicitar codigo de verificacao do atributo `email` para o usuario autenticado.
- Response: `email`, `emailVerified`, `emailVerificationRequired`, `status=CODE_SENT`, `attributeName`, `deliveryMedium`, `destination`.

### `POST /api/auth/email-verification/confirm`
- Request: `code`
- Uso: confirmar o codigo de verificacao do atributo `email` para o usuario autenticado.
- Response: `email`, `emailVerified`, `emailVerificationRequired`, `status=VERIFIED`, `attributeName`

### `POST /api/auth/logout`
- Uso: invalidar a sessao Cognito atual a partir do `access_token` autenticado.
- Response: `status=SIGNED_OUT`

### `GET /api/authz/check`
- Uso: diagnostico e validacao da matriz de autorizacao.
- Parametros principais: `module`, `action`, `resourceTenantId`, `resourceTenantType`, `targetRole`, `supportOverride`, `justification`.
- Resposta: `allowed`, `reason`, `restrictions`, `auditRequired`, `maskedViewRequired`, `crossTenant`, `timestamp`.

## Organizacoes
### `GET /api/portfolio/organizations`
- Filtros: `status`, `setupStatus`, `customerOrganizationId`, `parentOrganizationId`, `hierarchyLevel`, `search`.
- Resposta: lista de `OrganizationResponse`.
- Campos principais: `id`, `name`, `code`, `tenantType`, `parentOrganizationId`, `customerOrganizationId`, `hierarchyLevel`, `childrenCount`, `hasChildren`, `status`, `setupStatus`, `userSummary`, `programSummary`, `canInactivate`, `inactivationBlockedReason`, `createdAt`, `updatedAt`.

### `GET /api/portfolio/organizations/{organizationId}`
- Uso: detalhe administrativo de organizacao.
- Resposta: mesmo shape de `OrganizationResponse`.

### `POST /api/portfolio/organizations`
- Request: `name`, `code`, `parentOrganizationId`, `status`.
- Observacao: `parentOrganizationId=null` cria customer raiz e exige `ADMIN INTERNAL`.
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
- Observacao: `reset-access` retorna `409` quando o usuario ainda nao possui canal de recovery verificado no Cognito e `429` quando o Cognito throttla a operacao.

### `UserSummaryResponse`
- `id`, `displayName`, `email`, `role`, `organizationId`, `organizationName`, `status`, `createdAt`, `inviteResentAt`, `accessResetAt`

## Portfolio
### Milestone templates
- `GET /api/portfolio/milestone-templates`
- `POST /api/portfolio/milestone-templates`
- Request principal: `name`, `description`, `status`, `items[]`.

### Programs
- `GET /api/portfolio/programs`
- Filtro: `ownerOrganizationId`
- Resposta resumida: `ProgramSummaryResponse`
- `POST /api/portfolio/programs`
- Request principal: `name`, `code`, `description`, `ownerOrganizationId`, `plannedStartDate`, `plannedEndDate`, `participants[]`, `initialProject`
- `GET /api/portfolio/programs/{programId}`
- Resposta detalhada agregada: participantes, projetos, milestones, produtos, itens, entregaveis, documentos e open issues.

### Camadas de criacao
- `POST /api/portfolio/programs/{programId}/projects`
- `POST /api/portfolio/projects/{projectId}/products`
- `POST /api/portfolio/products/{productId}/items`
- `POST /api/portfolio/items/{itemId}/deliverables`
- `POST /api/portfolio/programs/{programId}/open-issues`

### Documentos de entregavel
- `GET /api/portfolio/deliverables/{deliverableId}/documents`
- Response: lista de `DeliverableDocumentResponse`
- `POST /api/portfolio/deliverables/{deliverableId}/documents/upload-url`
- Request: `PrepareDeliverableDocumentUploadRequest`
- Response atual: `document`, `uploadUrl`, `expiresAt`, `requiredHeaders`
- Implementacao atual do backend assina um `PUT` para `uploadUrl`; o frontend precisa enviar o binario com `requiredHeaders` antes de chamar `complete`.
- `POST /api/portfolio/deliverables/{deliverableId}/documents/{documentId}/complete`
- Response: `DeliverableDocumentResponse`
- `POST /api/portfolio/deliverables/{deliverableId}/documents/{documentId}/download-url`
- Response atual: `documentId`, `downloadUrl`, `expiresAt`
- `DELETE /api/portfolio/deliverables/{deliverableId}/documents/{documentId}`
- Response: `DeliverableDocumentResponse`

### Requests principais do portfolio
- `CreateProjectRequest`: `name`, `code`, `description`, `plannedStartDate`, `plannedEndDate`, `milestoneTemplateId`, `status`
- `CreateProductRequest`: `name`, `code`, `description`, `status`
- `CreateItemRequest`: `name`, `code`, `description`, `status`
- `CreateDeliverableRequest`: `name`, `description`, `type`, `status`, `plannedDate`, `dueDate`
- `CreateOpenIssueRequest`: `title`, `description`, `severity`, `status`, `openedAt`
- `PrepareDeliverableDocumentUploadRequest`: `fileName`, `contentType`, `fileSize`
- `DeliverableDocumentResponse`: `id`, `deliverableId`, `fileName`, `contentType`, `fileSize`, `status`, `uploadedAt`, `createdAt`, `updatedAt`

## Operations
- `GET /api/operations`
- `POST /api/operations`
- `PUT /api/operations/{operationId}`
- `DELETE /api/operations/{operationId}`
- `POST /api/operations/{operationId}/submit`
- `POST /api/operations/{operationId}/approve`
- `POST /api/operations/{operationId}/reject`
- `POST /api/operations/{operationId}/reopen`
- `POST /api/operations/{operationId}/reprocess`
- `CreateOperationRequest`: `title`, `description`, `tenantId`, `tenantType`
- `UpdateOperationRequest`: `title`, `description`

## Reports
- `GET /api/reports/summary`
- `GET /api/reports/operations`
- `GET /api/reports/operations/export`
- Parametros relevantes: `tenantId`, `status`, `includeSensitiveData`, `maskedView`, `supportOverride`, `justification`

## Enums expostos de forma relevante
- `Role`: `ADMIN`, `MANAGER`, `MEMBER`, `SUPPORT`, `AUDITOR`
- `TenantType`: `INTERNAL`, `EXTERNAL`
- `UserStatus`: `INVITED`, `ACTIVE`, `INACTIVE`
- `OrganizationStatus`: `ACTIVE`, `INACTIVE`
- `OrganizationSetupStatus`: `COMPLETED`, `INCOMPLETED`
- `ProgramStatus`: `DRAFT`, `ACTIVE`, `PAUSED`, `CLOSED`, `CANCELED`
- `ParticipationRole`: `CLIENT`, `SUPPLIER`, `INTERNAL`, `PARTNER`
- `ParticipationStatus`: `ACTIVE`, `INACTIVE`
- `ProjectStatus`: `DRAFT`, `ACTIVE`, `PAUSED`, `COMPLETED`, `CANCELED`
- `ProductStatus`: `ACTIVE`, `INACTIVE`
- `ItemStatus`: `ACTIVE`, `INACTIVE`
- `DeliverableType`: `DOCUMENT`, `FORM`
- `DeliverableStatus`: `PENDING`, `IN_PROGRESS`, `SUBMITTED`, `APPROVED`, `REJECTED`, `CANCELED`
- `DeliverableDocumentStatus`: `PENDING_UPLOAD`, `AVAILABLE`, `DELETED`
- `MilestoneTemplateStatus`: `ACTIVE`, `INACTIVE`
- `ProjectMilestoneStatus`: `PLANNED`, `COMPLETED`, `CANCELED`
- `OpenIssueStatus`: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELED`
- `OpenIssueSeverity`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- `OperationStatus`: `DRAFT`, `SUBMITTED`, `APPROVED`, `REJECTED`, `RETURNED`, `REPROCESSING`

## Paginacao e busca
- Nao ha paginacao no contrato atual.
- Busca textual esta presente apenas em `GET /api/portfolio/organizations` via `search`.
- Crescimento de volume e paginação futura permanecem em `OPEN_GAPS.md`.
