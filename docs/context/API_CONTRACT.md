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
- `GET /api/authz/check`

Removed endpoint families:
- `/api/portfolio/**`
- `/api/operations/**`
- `/api/reports/**`
- legacy `/api/users/**`

## Core contract guarantees
- `/api/auth/me` and `/api/access/*` remain the supported public core surface.
- hardening changes preserve route shape and payload compatibility for the active core.
- lifecycle, retention, quota and export state are internal server-side controls unless noted otherwise below.

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
- callers must stop using `/api/portfolio/**`, `/api/operations/**` and `/api/reports/**`
- permission lists returned by `/api/auth/me` and membership endpoints no longer include `portfolio.*`, `operations.*` or `reports.*`
