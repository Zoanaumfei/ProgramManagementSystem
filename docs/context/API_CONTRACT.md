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

Legacy claim echo fields were removed. The access context is always resolved from local membership data.

## User account admin
`/api/access/users` manages the global user account lifecycle only.

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

Important:
- effective authorization is not read from `app_user`
- `app_user` is identity-only and does not persist tenant or role snapshots
- the membership APIs remain the source of truth for contextual access
- `membershipAssigned` indicates whether the user already has at least one resolvable membership

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
- this route is valid only when the target user has no memberships yet
- after the first assignment, further access changes happen through `/api/access/users/{userId}/memberships`

## Organization admin
Organization hierarchy management is part of the active access core.

Supported route family:
- `/api/access/organizations`

Important:
- organization administration no longer lives under the portfolio namespace
- organization responses may still expose setup/program summary fields, but the portfolio runtime itself is frozen
- legacy `/api/portfolio/**` requests now return `503 Service Unavailable`

## Markets
Markets are tenant-scoped and can be inactivated only when not referenced by active memberships or organizations.

## Breaking contract notes
- `POST /api/access/users` and `PUT /api/access/users/{userId}` no longer accept `role` or `organizationId`
- clients that previously created a user and membership in one payload must now call `/api/access/users` first and `/api/access/users/{userId}/bootstrap-membership` second
