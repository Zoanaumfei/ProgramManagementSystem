# API Contract

## Supported endpoints
- `GET /api/auth/me`
- `POST /api/access/context/activate`
- `GET /api/access/users`
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
- `POST /api/access/organizations`
- `PUT /api/access/organizations/{organizationId}`
- `DELETE /api/access/organizations/{organizationId}`
- `POST /api/access/organizations/{organizationId}/purge-subtree`
- `GET /api/access/tenants`
- `GET /api/access/tenants/{tenantId}/markets`
- `POST /api/access/tenants/{tenantId}/markets`
- `PUT /api/access/tenants/{tenantId}/markets/{marketId}`
- `DELETE /api/access/tenants/{tenantId}/markets/{marketId}`
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

## User account admin
`/api/access/users` manages global user lifecycle only.

Request shape for create/update:
```json
{
  "displayName": "Jane Doe",
  "email": "jane@customer.com"
}
```

Response shape:
```json
{
  "id": "USR-...",
  "displayName": "Jane Doe",
  "email": "jane@customer.com",
  "status": "INVITED",
  "membershipAssigned": false,
  "createdAt": "...",
  "inviteResentAt": null,
  "accessResetAt": null
}
```

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
`POST /api/access/users/{userId}/bootstrap-membership` assigns the first membership to a lifecycle-only user.

Request shape:
```json
{
  "organizationId": "tenant-a",
  "marketId": null,
  "status": "ACTIVE",
  "roles": ["ADMIN"]
}
```

Important:
- valid only when the target user has no memberships yet
- after the first assignment, further access changes happen through `/api/access/users/{userId}/memberships`
- bootstrap can return `409 Conflict` when the target tenant has reached its active-membership quota

## Organization admin
Organization hierarchy management is part of the active access core.

`OrganizationResponse` includes:
- `id`
- `name`
- `code`
- `tenantId`
- `marketId`
- `tenantType`
- `parentOrganizationId`
- `customerOrganizationId`
- `hierarchyLevel`
- `childrenCount`
- `hasChildren`
- `status`
- `setupStatus`
- `userSummary`
- `canInactivate`
- `inactivationBlockedReason`
- `createdAt`
- `updatedAt`

`OrganizationPurgeResponse` includes:
- `organizationId`
- `action`
- `performedAt`
- `status`
- `purgedOrganizations`
- `purgedUsers`

Important behavior:
- `DELETE /api/access/organizations/{organizationId}` now performs subtree offboarding instead of requiring prior manual user revocation
- offboarding revokes memberships, disables users that lose all active memberships and marks the organization subtree for retention/export handling internally
- destructive deletion is still explicit through `POST /api/access/organizations/{organizationId}/purge-subtree`
- child organization creation can return `409 Conflict` when tenant organization quota is exhausted

## Markets
Markets are tenant-scoped and can be inactivated only when not referenced by active memberships or organizations.

Important behavior:
- market creation can return `409 Conflict` when the tenant market quota is exhausted

## Breaking contract notes
- `POST /api/access/users` and `PUT /api/access/users/{userId}` no longer accept `role` or `organizationId`
- clients that previously created a user and membership in one payload must call `/api/access/users` first and `/api/access/users/{userId}/bootstrap-membership` second
- `OrganizationResponse` no longer exposes any program or portfolio summary block
- `OrganizationPurgeResponse` no longer exposes purged program or document counters
- callers must stop using `/api/portfolio/**`, `/api/operations/**` and `/api/reports/**`
- permission lists returned by `/api/auth/me` and membership endpoints no longer include `portfolio.*`, `operations.*` or `reports.*`
