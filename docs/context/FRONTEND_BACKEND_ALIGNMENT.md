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
- frontend should rely on structured `401`/`403` payloads with `message`, `path` and `correlationId` to explain authorization failures to admins

## Current frontend status
- the main `/workspace` shell has been migrated to the access-first flow
- users, organizations, markets and session diagnostics now route through the access shell
- the legacy `src/features/portfolio` subtree was removed from the frontend source tree during the cleanup
- the users page no longer depends on an organization directory query to render its main workflow
- the backend contract already defines `GET /api/access/users`, but the published environment used during frontend testing still returned a static-resource-style 404 for that route
- the users page now includes a dedicated `Usuários sem membership` section that will consume `/api/access/users/orphans` when the backend read model is available
- the backend now exposes `GET /api/access/users/orphans` and organization relationship endpoints; the frontend should wire them into bootstrap and organization management screens
