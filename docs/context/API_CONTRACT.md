# API Contract

## Supported endpoints
- `GET /api/auth/me`
- `POST /api/access/context/activate`
- `GET /api/access/users`
- `GET /api/access/users/orphans`
- `POST /api/access/users`
- `PUT /api/access/users/{userId}`
- `DELETE /api/access/users/{userId}`
- `POST /api/access/users/{userId}/resend-invite`
- `POST /api/access/users/{userId}/reset-access`
- `POST /api/access/users/{userId}/purge`
- `POST /api/access/users/{userId}/bootstrap-membership`
- `GET /api/access/users/{userId}/memberships`
- `POST /api/access/users/{userId}/memberships`
- `PUT /api/access/users/{userId}/memberships/{membershipId}`
- `DELETE /api/access/users/{userId}/memberships/{membershipId}`
- `GET /api/access/organizations`
- `GET /api/access/organizations/{organizationId}`
- `GET /api/access/organizations/{organizationId}/relationships`
- `POST /api/access/organizations/{organizationId}/relationships`
- `PUT /api/access/organizations/{organizationId}/relationships/{relationshipId}`
- `DELETE /api/access/organizations/{organizationId}/relationships/{relationshipId}`
- `POST /api/access/organizations`
- `PUT /api/access/organizations/{organizationId}`
- `DELETE /api/access/organizations/{organizationId}`
- `POST /api/access/organizations/{organizationId}/purge-subtree`
- `GET /api/access/tenants`
- `PATCH /api/access/tenants/{tenantId}/service-tier`
- `GET /api/access/tenants/{tenantId}/markets`
- `POST /api/access/tenants/{tenantId}/markets`
- `PUT /api/access/tenants/{tenantId}/markets/{marketId}`
- `DELETE /api/access/tenants/{tenantId}/markets/{marketId}`
- `GET /api/access/organizations/{organizationId}/exports`
- `POST /api/access/organizations/{organizationId}/exports`
- `PATCH /api/access/organizations/{organizationId}/exports`
- `GET /api/admin/operational/overview`
- `GET /api/authz/check`
- `POST /api/projects`
- `GET /api/projects`
- `GET /api/projects/{projectId}`
- `PATCH /api/projects/{projectId}`
- `POST /api/projects/{projectId}/purge-intents`
- `POST /api/projects/{projectId}/purge`
- `POST /api/projects/{projectId}/organizations`
- `GET /api/projects/{projectId}/organizations`
- `POST /api/projects/{projectId}/members`
- `GET /api/projects/{projectId}/members`
- `GET /api/projects/{projectId}/milestones`
- `PATCH /api/projects/{projectId}/milestones/{milestoneId}`
- `GET /api/projects/{projectId}/deliverables`
- `GET /api/projects/{projectId}/deliverables/pending-review`
- `GET /api/projects/{projectId}/deliverables/{deliverableId}`
- `PATCH /api/projects/{projectId}/deliverables/{deliverableId}`
- `POST /api/projects/{projectId}/deliverables/{deliverableId}/submissions`
- `GET /api/projects/{projectId}/deliverables/{deliverableId}/submissions`
- `GET /api/projects/{projectId}/deliverables/{deliverableId}/submissions/{submissionId}`
- `POST /api/projects/{projectId}/deliverables/{deliverableId}/submissions/{submissionId}/approve`
- `POST /api/projects/{projectId}/deliverables/{deliverableId}/submissions/{submissionId}/reject`
- `GET /api/projects/{projectId}/dashboard`
- `GET /api/project-frameworks`
- `POST /api/project-frameworks`
- `PATCH /api/project-frameworks/{frameworkId}`
- `GET /api/project-templates`
- `GET /api/project-templates/{templateId}`
- `POST /api/project-templates`
- `PATCH /api/project-templates/{templateId}`
- `POST /api/project-templates/{templateId}/purge`
- `GET /api/project-templates/{templateId}/phases`
- `POST /api/project-templates/{templateId}/phases`
- `PATCH /api/project-templates/{templateId}/phases/{phaseTemplateId}`
- `GET /api/project-templates/{templateId}/milestones`
- `POST /api/project-templates/{templateId}/milestones`
- `PATCH /api/project-templates/{templateId}/milestones/{milestoneTemplateId}`
- `GET /api/project-templates/{templateId}/deliverables`
- `POST /api/project-templates/{templateId}/deliverables`
- `PATCH /api/project-templates/{templateId}/deliverables/{deliverableTemplateId}`
- `GET /api/project-structure-templates`
- `GET /api/project-structure-templates/{structureTemplateId}`
- `POST /api/project-structure-templates`
- `PATCH /api/project-structure-templates/{structureTemplateId}`
- `POST /api/project-structure-templates/{structureTemplateId}/purge`
- `POST /api/project-structure-templates/{structureTemplateId}/activate`
- `POST /api/project-structure-templates/{structureTemplateId}/deactivate`
- `POST /api/project-structure-templates/{structureTemplateId}/levels`
- `PATCH /api/project-structure-templates/{structureTemplateId}/levels/{levelTemplateId}`
- `POST /api/project-structure-templates/{structureTemplateId}/levels/reorder`
- `POST /api/document-contexts/{contextType}/{contextId}/documents/uploads`
- `GET /api/document-contexts/{contextType}/{contextId}/documents`
- `GET /api/documents/{documentId}`
- `POST /api/documents/{documentId}/finalize-upload`
- `POST /api/documents/{documentId}/download-url`
- `DELETE /api/documents/{documentId}`

## Core contract guarantees
- `/api/auth/me` and `/api/access/*` remain the supported public core surface.
- hardening changes preserve route shape and payload compatibility for the active core.
- lifecycle, retention, quota and export state are internal server-side controls unless noted otherwise below.

## Template administration
`/api/project-templates` and `/api/project-structure-templates` expose template governance for project setup with ownership and relationship-chain authorization.

## Project framework catalog
`/api/project-frameworks` exposes the catalog of project framework codes used by templates and projects plus the frontend UX/UI layout hint for each framework.

Important behavior:
- framework codes are now persisted catalog entries rather than a closed backend enum
- existing template and project payloads still use the `frameworkType` field, but it now carries the catalog `code`
- `GET /api/project-frameworks` is the source of truth for framework display metadata such as `displayName`, `description`, `uiLayout` and `active`
- only internal `ADMIN` actors can create or edit frameworks
- project framework management supports `create` and `update` only; there is no delete/purge route
- inactive frameworks remain visible in the catalog but cannot be used for new project, project-template or project-structure-template creation
- current `uiLayout` values are `TIMELINE`, `BOARD` and `HYBRID`

## Project administration
`/api/projects` now includes an explicit administrative purge flow for internal break-glass actors.

Important behavior:
- `GET /api/projects` remains tenant-scoped for normal actors, but internal `ADMIN` actors now receive a platform-wide project list
- direct project detail and update routes continue to honor the internal privileged cross-tenant access path already present in the backend authorization layer
- `POST /api/projects/{projectId}/purge-intents` creates a short-lived destructive intent and returns `purgeToken`, expiration metadata and a backend-calculated impact summary
- `POST /api/projects/{projectId}/purge` requires the original `reason`, `purgeToken`, `confirm=true` and the exact confirmation text `PURGE PROJECT`
- project purge is allowed only for internal `ADMIN` and `SUPPORT` actors
- project purge physically removes the project aggregate, participant rows, phases, milestones, deliverables, submissions, structure nodes, document bindings, document rows and storage objects discovered under the `PROJECT`, `PROJECT_DELIVERABLE` and `PROJECT_DELIVERABLE_SUBMISSION` contexts
- purge-intent expiration and token mismatch failures are returned as business-rule errors with stable `code` fields
- successful purge intent creation and project purge execution are audited as `PROJECT_PURGE_INTENT_CREATED`, `PROJECT_PURGE_EXECUTION_STARTED` and `PROJECT_PURGE_COMPLETED`

Important behavior:
- template ownership is fixed at creation time from the actor active organization context (`ownerOrganizationId` in the backend model)
- `GET /api/project-templates` and `GET /api/project-structure-templates` return only templates the active organization can use
- `GET /api/project-templates/{templateId}` and `GET /api/project-structure-templates/{structureTemplateId}` are allowed only when the active organization can use the template
- template use is allowed when the active organization is the owner or a descendant in the active `CUSTOMER_SUPPLIER` relationship chain
- management operations (`create/update/purge/activate/deactivate` and template-child mutations such as phases/milestones/deliverables/levels) are allowed only for owner organization admins
- `POST /api/project-templates/{templateId}/purge` permanently removes a project template plus its template-owned phases, milestones and deliverables.
- project-template purge is rejected with business code `PROJECT_TEMPLATE_DEFAULT_CANNOT_BE_PURGED` when the template is still the default for its framework.
- project-template purge is rejected with business code `PROJECT_TEMPLATE_IN_USE` when any project already references that template.
- `POST /api/project-structure-templates/{structureTemplateId}/purge` permanently removes a structure template plus its structure levels.
- structure-template purge is rejected with business code `PROJECT_STRUCTURE_TEMPLATE_IN_USE` when any project template still references that structure template.
- successful purge operations are audited by the backend as `PROJECT_TEMPLATE_PURGED` and `PROJECT_STRUCTURE_TEMPLATE_PURGED`.

## Standard error payloads
Structured backend errors now use the following shape whenever the security and exception layers provide a JSON response:
- `timestamp`
- `status`
- `error`
- `message`
- `path`
- `correlationId`

Important behavior:
- `401 Unauthorized` and `403 Forbidden` now ship in this structured format
- business-rule and validation failures may also include stable `code` values and optional `details`
- the frontend should surface `message` and `correlationId` directly to admins when useful

## Auth me
`GET /api/auth/me` returns the authenticated identity plus resolved access context.

Fields:
- `subject`
- `username`
- `email`
- `emailVerified`
- `emailVerificationRequired`
- `tokenUse`
- `userId`
- `membershipId`
- `activeTenantId`
- `activeTenantName`
- `activeOrganizationId`
- `activeOrganizationName`
- `activeMarketId`
- `activeMarketName`
- `tenantType`
- `roles`
- `permissions`
- `groups`
- `scopes`
- `authorities`
- `timestamp`

Important behavior:
- legacy claim echo fields were removed
- access context is always resolved from local membership data
- invalid `X-Access-Context` values now return `400 Bad Request`
- tenant rate limiting may return `429 Too Many Requests`
- tenant rate limiting is backed by a shared counter store in production and falls back to local counters only when `app.multitenancy.rate-limit.store=local`

## User account admin
`/api/access/users` manages global user lifecycle only.

Create request shape:
```json
{
  "displayName": "Jane Doe",
  "email": "jane@customer.com",
  "organizationId": "ORG-123",
  "marketId": null,
  "roles": ["MEMBER"]
}
```

Update request shape:
```json
{
  "displayName": "Jane Doe Updated",
  "email": "jane.updated@customer.com"
}
```

Representative response shape:
```json
{
  "id": "USR-...",
  "displayName": "Jane Doe",
  "email": "jane@customer.com",
  "status": "INVITED",
  "membershipAssigned": true,
  "createdAt": "...",
  "inviteResentAt": null,
  "accessResetAt": null
}
```

`GET /api/access/users/orphans` returns inconsistent users without memberships when the actor is authorized to discover repair targets.

Important behavior:
- `POST /api/access/users` now provisions the first membership in the same flow
- `organizationId` and `roles` are required for create
- `marketId` is optional
- if the initial membership cannot be provisioned, create fails closed and may return business code `USER_CREATION_MEMBERSHIP_FAILED`
- `PUT /api/access/users/{userId}` updates only lifecycle/identity fields (`displayName`, `email`)
- newly created users should no longer require `/bootstrap-membership` during the standard invite -> temporary password -> first login path
- lifecycle operations that find a user with no memberships return business code `ORPHAN_USER_DETECTED`
- lifecycle operations that require an active membership context can return `USER_ACTIVE_MEMBERSHIP_REQUIRED`
- `GET /api/access/users/orphans` remains a legacy/inconsistency discovery surface, not the normal create flow

## Membership admin
Membership is the first-class access resource.

`MembershipResponse` includes:
- `id`
- `userId`
- `tenantId`
- `tenantName`
- `organizationId`
- `organizationName`
- `marketId`
- `marketName`
- `status`
- `defaultMembership`
- `joinedAt`
- `updatedAt`
- `roles`
- `permissions`

Important behavior:
- `DELETE /api/access/users/{userId}/memberships/{membershipId}` now performs membership offboarding
- offboarding revokes access immediately and audits the operation
- if the user loses the last active membership, the user account is inactivated as part of the same flow
- active membership creation can return `409 Conflict` when tenant quota is exhausted

## First membership bootstrap
`POST /api/access/users/{userId}/bootstrap-membership` assigns the first membership only during exceptional repair of an inconsistent user record.

Request shape:
```json
{
  "organizationId": "ORG-123",
  "marketId": null,
  "status": "ACTIVE",
  "roles": ["ADMIN"]
}
```

Important:
- valid only when the target user has no memberships yet
- after the first assignment, further access changes happen through `/api/access/users/{userId}/memberships`
- bootstrap can return `409 Conflict` when the target tenant has reached its active-membership quota
- bootstrap is not part of the standard onboarding flow anymore

## Organization admin
Organization relationship management is part of the active access core.

`OrganizationResponse` includes:
- `id`
- `name`
- `cnpj`
- `tenantId`
- `marketId`
- `tenantType`
- `childrenCount`
- `hasChildren`
- `status`
- `setupStatus`
- `userSummary`
- `canInactivate`
- `inactivationBlockedReason`
- `createdAt`
- `updatedAt`
- `reused`

`GET /api/access/organizations/{organizationId}/relationships` returns active organization relationships.

`OrganizationRelationshipResponse` includes:
- `id`
- `sourceOrganizationId`
- `targetOrganizationId`
- `relationshipType`
- `localOrganizationCode`
- `status`
- `createdAt`
- `updatedAt`

`POST /api/access/organizations/{organizationId}/relationships` accepts:
```json
{
  "targetOrganizationId": "ORG-123",
  "relationshipType": "CUSTOMER_SUPPLIER",
  "localOrganizationCode": "DELGA-VW-001"
}
```

`PUT /api/access/organizations/{organizationId}/relationships/{relationshipId}` accepts:
```json
{
  "localOrganizationCode": "DELGA-VW-002"
}
```

`DELETE /api/access/organizations/{organizationId}/relationships/{relationshipId}` inactivates the relationship and returns the updated relationship snapshot.

`OrganizationPurgeResponse` includes:
- `organizationId`
- `action`
- `performedAt`
- `status`
- `purgedOrganizations`
- `purgedUsers`

Important behavior:
- organization identity inside a tenant is canonicalized by `cnpj`
- `POST /api/access/organizations` now requires `name` and `cnpj`
- `POST /api/access/organizations` may also receive optional `localOrganizationCode`
- when an `EXTERNAL ADMIN` creates an organization, the backend creates or reuses the supplier organization by `tenantId + cnpj`
- when an existing organization is reused, the backend creates or reactivates the `CUSTOMER_SUPPLIER` relationship from the actor organization to the reused organization
- `localOrganizationCode` is relationship-local metadata, not canonical organization identity
- if provided, `localOrganizationCode` must be unique per `sourceOrganizationId`
- create and relationship validation errors can now include a stable business `code` plus optional `details`
- relationship edges are now the explicit source of truth for organization visibility and subtree traversal
- the legacy tree fields `parentOrganizationId`, `customerOrganizationId` and `hierarchyLevel` are no longer part of the public contract
- relationship inactivation keeps the historical row with `status=INACTIVE`; it is not a physical delete
- `DELETE /api/access/organizations/{organizationId}` now performs subtree offboarding instead of requiring prior manual user revocation
- offboarding revokes memberships, disables users that lose all active memberships and marks the organization subtree for retention/export handling internally
- destructive deletion is still explicit through `POST /api/access/organizations/{organizationId}/purge-subtree`
- `POST /api/access/organizations/{organizationId}/purge-subtree` now removes both active and inactive relationship edges that reference the subtree before deleting the organizations
- organization creation can return `409 Conflict` when tenant organization quota is exhausted
- relationship creation rejects self-links and cycles in the `CUSTOMER_SUPPLIER` graph
- relationship create/update can reject duplicated `localOrganizationCode` for the same source organization with business code `ORGANIZATION_RELATIONSHIP_LOCAL_CODE_ALREADY_EXISTS`

## Markets
Markets are tenant-scoped and can be inactivated only when not referenced by active memberships or organizations.

Important behavior:
- market creation can return `409 Conflict` when the tenant market quota is exhausted

## Tenant directory
`GET /api/access/tenants` currently returns a tenant summary read model.

`TenantSummaryResponse` includes:
- `id`
- `name`
- `code`
- `status`
- `tenantType`
- `rootOrganizationId`

## Tenant service tier
`PATCH /api/access/tenants/{tenantId}/service-tier` updates the tenant tier through an audited backoffice flow.

Request shape:
```json
{
  "serviceTier": "ENTERPRISE",
  "justification": "Increase tenant capacity for seasonal load."
}
```

Response shape:
```json
{
  "tenantId": "TEN-tenant-a",
  "tenantName": "Tenant A",
  "previousServiceTier": "STANDARD",
  "serviceTier": "ENTERPRISE",
  "updatedAt": "..."
}
```

Important behavior:
- the endpoint is restricted to authorized `ADMIN` and `SUPPORT` actors
- `SUPPORT` requests require audit trail support and a justification
- invalid transitions return `409 Conflict`
- no-op changes to the current tier return `409 Conflict`
- the change is audited with actor, tenant target, previous tier, new tier and justification
- the updated tier is used immediately for quota and rate-limit policy resolution

## Organization export operations
`/api/access/organizations/{organizationId}/exports` exposes the audited operator-facing export surface for offboarded organizations.

Request shape:
```json
{
  "justification": "Manual export requested for retention handling."
}
```

Response shape:
```json
{
  "organizationId": "ORG-123",
  "lifecycleState": "OFFBOARDED",
  "dataExportStatus": "EXPORT_IN_PROGRESS",
  "eligible": true,
  "offboardedAt": "...",
  "retentionUntil": "...",
  "dataExportedAt": null,
  "updatedAt": "..."
}
```

Important behavior:
- `GET` returns the current export status snapshot for an organization
- `POST` transitions `READY_FOR_EXPORT -> EXPORT_IN_PROGRESS`
- `PATCH` transitions `EXPORT_IN_PROGRESS -> EXPORTED`
- the organization must be `OFFBOARDED` and still inside the retention window
- each transition is audited with actor, target organization and justification
- attempts outside the eligibility window return `409 Conflict`

## Breaking contract notes
- `POST /api/access/users` now requires `organizationId` and `roles`, and may include `marketId`
- organization payloads no longer accept or return organization-level `code`
- relationship payloads/responses now carry `localOrganizationCode`
- `OrganizationResponse` no longer exposes any program or portfolio summary block
- `OrganizationPurgeResponse` no longer exposes purged program or document counters

## Project management
`/api/projects` is now the active collaborative project-management surface.

Create request shape:
```json
{
  "code": "PRJ-APQP-001",
  "name": "Project PRJ-APQP-001",
  "description": "integration flow",
  "frameworkType": "APQP",
  "templateId": "TMP-APQP-V1",
  "customerOrganizationId": "ORG-123",
  "status": "PLANNED",
  "visibilityScope": "ALL_PROJECT_PARTICIPANTS",
  "plannedStartDate": "2026-04-08",
  "plannedEndDate": "2026-06-30"
}
```

Important behavior:
- `Idempotency-Key` is supported on create and submission/review mutation flows
- when `templateId` is omitted, the backend resolves the current default template for the chosen `frameworkType` inside the actor authorized template catalog
- when `templateId` is provided, the backend validates template-use authorization for the active organization context before creating the project
- the lead organization is always derived from the authenticated active access context, not from a free-form request field
- project update, milestone update, deliverable update and submission review flows use optimistic concurrency through `version`

`ProjectSummaryResponse` includes:
- `id`
- `code`
- `name`
- `frameworkType`
- `status`
- `visibilityScope`
- `leadOrganizationId`
- `customerOrganizationId`
- `plannedStartDate`
- `plannedEndDate`
- `createdAt`

`ProjectDetailResponse` includes:
- `id`
- `code`
- `name`
- `description`
- `frameworkType`
- `templateId`
- `templateVersion`
- `leadOrganizationId`
- `customerOrganizationId`
- `status`
- `visibilityScope`
- `plannedStartDate`
- `plannedEndDate`
- `actualStartDate`
- `actualEndDate`
- `createdByUserId`
- `createdAt`
- `updatedAt`
- `version`
- `organizations`
- `members`

`ProjectDashboardResponse` includes:
- `projectId`
- `totalDeliverables`
- `pendingSubmissionCount`
- `pendingReviewCount`
- `approvedCount`
- `rejectedCount`
- `overdueCount`
- `milestonesAtRisk`
- `nextMilestoneDate`

`ProjectMilestoneResponse` includes:
- `id`
- `phaseId`
- `code`
- `name`
- `sequence`
- `plannedDate`
- `actualDate`
- `status`
- `ownerOrganizationId`
- `visibilityScope`
- `version`

`ProjectDeliverableResponse` includes:
- `id`
- `phaseId`
- `milestoneId`
- `code`
- `name`
- `description`
- `deliverableType`
- `responsibleOrganizationId`
- `responsibleUserId`
- `approverOrganizationId`
- `approverUserId`
- `requiredDocument`
- `plannedDueDate`
- `submittedAt`
- `approvedAt`
- `status`
- `priority`
- `visibilityScope`
- `version`

Deliverable submission create request:
```json
{
  "deliverableVersion": 0,
  "documentIds": ["DOC-123"]
}
```

Deliverable review request:
```json
{
  "reviewComment": "Looks good",
  "version": 0
}
```

`DeliverableSubmissionResponse` includes:
- `id`
- `submissionNumber`
- `submittedByUserId`
- `submittedByOrganizationId`
- `submittedAt`
- `status`
- `reviewComment`
- `reviewedByUserId`
- `reviewedAt`
- `version`
- `documentIds`

## Document management
The active document-management contexts relevant to project-management are:
- `PROJECT`
- `PROJECT_DELIVERABLE`
- `PROJECT_DELIVERABLE_SUBMISSION`

Upload-initiation request shape:
```json
{
  "originalFilename": "evidence.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 2048,
  "checksumSha256": "..."
}
```

`UploadSessionResponse` includes:
- `documentId`
- `url`
- `fields`
- `expiresAt`

`DocumentResponse` includes:
- `id`
- `tenantId`
- `contextType`
- `contextId`
- `ownerOrganizationId`
- `originalFilename`
- `safeFilename`
- `contentType`
- `extension`
- `sizeBytes`
- `checksumSha256`
- `status`
- `uploadedByUserId`
- `uploadedByOrganizationId`
- `createdAt`
- `updatedAt`
- `deletedAt`

Important behavior:
- the frontend must upload the raw file directly to the returned presigned POST target, then call `POST /api/documents/{documentId}/finalize-upload`
- the backend validates filename extension, content type, checksum and exact file size during initiation/finalize
- current file policy allows office files, pdf, csv/txt and common raster image formats, and rejects executable/compressed/cad-oriented extensions
- the default max file size is `26214400` bytes (`25 MB`)
