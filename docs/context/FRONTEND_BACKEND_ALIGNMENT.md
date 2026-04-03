# Frontend Backend Alignment

This repository now exposes only the core membership-first platform surface.

## Backend contract verified in this repository
- current user and active context: `GET /api/auth/me`
- request-scoped context switch: `POST /api/access/context/activate`
- user account admin: `/api/access/users`
- orphan cleanup discovery: `GET /api/access/users/orphans`
- membership admin: `/api/access/users/{userId}/memberships`
- organization admin: `/api/access/organizations`
- organization relationships: `/api/access/organizations/{organizationId}/relationships`
- organization export operations: `/api/access/organizations/{organizationId}/exports`
- tenant directory: `GET /api/access/tenants`
- tenant service tier updates: `PATCH /api/access/tenants/{tenantId}/service-tier`
- tenant market admin: `/api/access/tenants/{tenantId}/markets`

## Backend behaviors the frontend must honor
- frontend should treat membership APIs as the main access-management surface
- frontend should treat organization administration as part of the core access shell
- frontend must not call `/api/portfolio/**`, `/api/operations/**` or `/api/reports/**`
- frontend should send `X-Access-Context` when operating under a request-scoped selected membership
- invalid `X-Access-Context` values fail closed with `400 Bad Request`
- frontend should rely on structured `401`/`403` payloads with `message`, `path` and `correlationId` to explain authorization failures to admins
- frontend should require `organizationId` and `roles` when creating users because the backend now provisions the first membership in the same request
- `GET /api/access/users/orphans` is a cleanup/discovery surface for inconsistent data only, not the normal onboarding path
- frontend should treat business codes such as `ORPHAN_USER_DETECTED`, `USER_ACTIVE_MEMBERSHIP_REQUIRED` and `USER_CREATION_MEMBERSHIP_FAILED` as actionable data-repair errors rather than as recoverable inline onboarding states
- frontend users copy should frame `Usuarios sem membership` as `diagnostico e reparo`, and the memberships workspace should label `bootstrap-membership` as an exceptional repair action instead of a standard create step
- frontend administrative error messaging should include backend `correlationId` whenever those business-rule failures are shown to operators

## Organization model alignment
- frontend should require `name` and `cnpj` in organization create/update flows
- frontend should treat `cnpj` as canonical organization identity inside the active tenant
- frontend should treat organization create as a register-or-link flow by `tenant + cnpj`, with relationship creation handled by the backend when needed
- frontend should treat `localOrganizationCode` as optional metadata owned by the relationship, not the organization
- frontend should not use `parentOrganizationId`, `customerOrganizationId` or `hierarchyLevel` in forms, grids, filters, trees or breadcrumbs because those legacy hierarchy fields are no longer part of the public contract
- frontend may continue to use `childrenCount` and `hasChildren` for summaries, but only as derived relationship counters rather than as proof of a rigid tree model
- frontend copy for external organization onboarding should prefer wording such as `Cadastrar ou vincular organizacao` instead of promising that every request creates a brand-new record
- frontend should treat `PARTNER` relationships as explicit graph edges and avoid implying that they belong to the customer/supplier traversal chain

## User and membership alignment
- user create payload:

```json
{
  "displayName": "Jane Doe",
  "email": "jane@customer.com",
  "organizationId": "ORG-123",
  "marketId": null,
  "roles": ["MEMBER"]
}
```

- user update payload:

```json
{
  "displayName": "Jane Doe Updated",
  "email": "jane.updated@customer.com"
}
```

- membership create/update payloads remain tenant-scoped and include:

```json
{
  "tenantId": "TEN-tenant-a",
  "organizationId": "ORG-123",
  "marketId": null,
  "status": "ACTIVE",
  "defaultMembership": false,
  "roles": ["ADMIN"]
}
```

## Organization and relationship payload alignment
- organization create payload:

```json
{
  "name": "Gestamp",
  "cnpj": "12.345.678/0001-95",
  "status": "ACTIVE",
  "localOrganizationCode": "GESTAMP-VW-001"
}
```

- organization update payload:

```json
{
  "name": "Gestamp Brasil",
  "cnpj": "12.345.678/0001-95"
}
```

- relationship create payload:

```json
{
  "targetOrganizationId": "ORG-123",
  "relationshipType": "CUSTOMER_SUPPLIER",
  "localOrganizationCode": "DELGA-VW-001"
}
```

- relationship update payload:

```json
{
  "localOrganizationCode": "DELGA-VW-002"
}
```

## Backend response details relevant to the frontend
- `OrganizationResponse` now exposes `reused: true|false` so the UI can distinguish create versus link semantics without inferring from side effects
- the backend normalizes `cnpj` to a numeric 14-digit value in responses even when the request used a masked format
- relationship payloads and responses carry `localOrganizationCode`
- `GET /api/access/tenants` currently returns a summary model with `id`, `name`, `code`, `status`, `tenantType` and `rootOrganizationId`
- create and validation conflicts can include stable business codes such as `ORGANIZATION_CNPJ_ALREADY_EXISTS_IN_TENANT` and `ORGANIZATION_RELATIONSHIP_LOCAL_CODE_ALREADY_EXISTS`

## Frontend tracking notes
The active frontend source tree is not fully present in this repository, so the notes below are tracked alignment expectations plus the latest documented product status.

- the main workspace direction remains access-first, centered on users, organizations, markets and session diagnostics
- the users experience may expose a dedicated `Usuarios sem membership` diagnostic view backed by `/api/access/users/orphans`
- the current frontend users workspace already distinguishes normal onboarding from exceptional repair: create uses initial membership provisioning in the same request, while orphan repair routes operators into membership administration
- organization management is expected to use the directed relationship model and stop rendering a derived single-path breadcrumb for organization visibility
- frontend error helpers are expected to surface backend-provided refusal reasons and `correlationId`
- frontend administrative workspaces now surface structured `401`/`403` data from the backend without overwriting the refusal reason with a generic fallback; the visible copy preserves `message`, `path` and `correlationId` in users, organizations and session diagnostics flows
- frontend runtime config now upgrades `http` API bases to `https` when served from an `https` app origin so the published shell does not keep an insecure backend URL by accident

## Operational validations still pending
- the published backend environment was revalidated on `2026-04-01`: `GET /api/access/users` now returns structured `401 Unauthorized` for anonymous requests, confirming that the route is served by the secured backend controller path rather than a static-resource `404`
- validate the published environment end-to-end so the frontend consistently receives the structured `401`/`403` payloads already implemented in the backend
- confirm the desired UX for `PARTNER` relationships in organization and user workspaces: explicit read-only distinction versus relying only on authorization failures
