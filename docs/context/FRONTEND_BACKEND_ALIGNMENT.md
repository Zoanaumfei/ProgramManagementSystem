# Frontend Backend Alignment

This repository now exposes only the core membership-first backend contract.

## Expected frontend integration points
- current user and active context: `GET /api/auth/me`
- request-scoped context switch: `POST /api/access/context/activate`
- user account admin: `/api/access/users`
- orphan bootstrap discovery: `GET /api/access/users/orphans`
- membership admin: `/api/access/users/{userId}/memberships`
- organization admin: `/api/access/organizations`
- organization relationships: `/api/access/organizations/{organizationId}/relationships`
- tenant directory: `GET /api/access/tenants`
- tenant market admin: `/api/access/tenants/{tenantId}/markets`

## Important backend expectations
- frontend should treat membership APIs as the main access-management surface
- frontend should treat organization administration as part of the core access shell
- frontend should remove portfolio, operations and reports menus that depended on removed backend routes
- frontend should not call `/api/portfolio/**`, `/api/operations/**` or `/api/reports/**`
- frontend should send `X-Access-Context` when operating under a request-scoped selected membership
- frontend should render organization-scoped orphan users from `/api/access/users/orphans` once the backend exposes the dedicated read model for bootstrap discovery
- frontend should render and manage organization relationships through `/api/access/organizations/{organizationId}/relationships` instead of relying on a purely parent/child create payload
- frontend should require `name`, `code` and `cnpj` in organization create flows, and `name`, `code` and `cnpj` in organization update flows
- frontend should treat `cnpj` as canonical organization identity inside the active tenant and `code` as globally unique across the platform
- frontend should treat organization create as a register-or-link flow by `tenant + cnpj`, with relationship creation handled by the backend
- frontend should not use `parentOrganizationId`, `customerOrganizationId` or `hierarchyLevel` in forms, grids, filters, trees or breadcrumbs because those legacy hierarchy fields are no longer part of the public contract
- frontend may continue to use `childrenCount` and `hasChildren` for summaries, but only as derived relationship counters rather than as proof of a rigid tree model
- frontend copy for external organization onboarding should prefer wording such as `Cadastrar ou vincular organização` instead of promising that every request creates a brand-new record
- frontend should rely on structured `401`/`403` payloads with `message`, `path` and `correlationId` to explain authorization failures to admins

## Organization handoff
- organization relationships are relationship-first: `CUSTOMER_SUPPLIER` edges are the source of truth for customer/supplier visibility
- an `EXTERNAL ADMIN` creating an organization can receive either a newly created organization or an existing tenant-scoped organization reused by `cnpj`
- when the backend reuses an existing organization for an `EXTERNAL ADMIN`, it automatically creates or reactivates the `CUSTOMER_SUPPLIER` relationship from the actor organization to the target organization
- the backend normalizes `cnpj` to a numeric 14-digit value in responses even when the request used a masked format
- the backend now exposes `reused: true|false` in `OrganizationResponse` so the UI can distinguish create vs link semantics without inferring from side effects

Expected create payload:

```json
{
  "name": "Gestamp",
  "code": "GESTAMP",
  "cnpj": "12.345.678/0001-95",
  "status": "ACTIVE"
}
```

Expected update payload:

```json
{
  "name": "Gestamp Brasil",
  "code": "GESTAMP-BR",
  "cnpj": "12.345.678/0001-95"
}
```

Representative organization response:

```json
{
  "id": "ORG-7877FBD67E99",
  "name": "Gestamp",
  "code": "GESTAMP",
  "cnpj": "12345678000195",
  "tenantId": "TEN-tenant-a",
  "marketId": null,
  "tenantType": "EXTERNAL",
  "childrenCount": 1,
  "hasChildren": true,
  "status": "ACTIVE",
  "setupStatus": "INCOMPLETED",
  "userSummary": {
    "invitedCount": 0,
    "activeCount": 0,
    "inactiveCount": 0,
    "totalCount": 0
  },
  "canInactivate": true,
  "inactivationBlockedReason": null,
  "createdAt": "2026-03-31T12:00:00Z",
  "updatedAt": "2026-03-31T12:00:00Z",
  "reused": false
}
```

Equivalent frontend validation/UX expectations:
- `name` required
- `code` required
- `cnpj` required
- `cnpj` mask recommended in the UI, but the frontend should sanitize it before any local comparison
- the frontend should prefer stable backend business codes such as `ORGANIZATION_CODE_ALREADY_EXISTS` and `ORGANIZATION_CNPJ_ALREADY_EXISTS_IN_TENANT` when present
- the frontend should be ready for conflict messages equivalent to `Ja existe uma organizacao com esse codigo.` and `Ja existe uma organizacao com esse CNPJ neste tenant.`

## Current frontend status
- the main `/workspace` shell has been migrated to the access-first flow
- users, organizations, markets and session diagnostics now route through the access shell
- the legacy `src/features/portfolio` subtree was removed from the frontend source tree during the cleanup
- the users page no longer depends on an organization directory query to render its main workflow
- the backend contract already defines `GET /api/access/users`, but the published environment used during frontend testing still returned a static-resource-style 404 for that route
- the users page now includes a dedicated `Usuários sem membership` section that will consume `/api/access/users/orphans` when the backend read model is available
- the backend now exposes `GET /api/access/users/orphans` and organization relationship endpoints; the frontend should wire them into bootstrap and organization management screens
- organization create/update payloads now include `cnpj`, and the old hierarchy fields are no longer part of `OrganizationResponse`
