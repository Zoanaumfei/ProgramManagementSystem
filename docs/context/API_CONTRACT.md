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
- `GET /api/access/users/{userId}/memberships`
- `POST /api/access/users/{userId}/memberships`
- `PUT /api/access/users/{userId}/memberships/{membershipId}`
- `DELETE /api/access/users/{userId}/memberships/{membershipId}`
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
`/api/access/users` manages the global user account plus the default access assignment used by the administrative flow.

Request shape for create/update:
```json
{
  "displayName": "Jane Doe",
  "email": "jane@customer.com",
  "role": "ADMIN",
  "organizationId": "tenant-a"
}
```

Response shape:
```json
{
  "id": "USR-...",
  "displayName": "Jane Doe",
  "email": "jane@customer.com",
  "role": "ADMIN",
  "organizationId": "tenant-a",
  "organizationName": "Tenant A",
  "status": "INVITED",
  "createdAt": "...",
  "inviteResentAt": null,
  "accessResetAt": null
}
```

Important:
- effective authorization is not read from `app_user`
- role and organization shown here are resolved and persisted through membership handling
- the membership APIs remain the source of truth for contextual access
- this endpoint is still a combined account-plus-initial-membership bootstrap surface; a future split to a pure `user_account` contract is still pending

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

## Markets
Markets are tenant-scoped and can be inactivated only when not referenced by active memberships or organizations.

## Pending contract cleanup
- `POST /api/access/users` and `PUT /api/access/users/{userId}` still accept `role` and `organizationId` to bootstrap the default membership
- the long-term target is to keep user-account lifecycle and membership assignment as separate API contracts
