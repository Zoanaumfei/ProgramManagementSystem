# Frontend Backend Alignment

This repository now exposes only the core membership-first backend contract.

## Expected frontend integration points
- current user and active context: `GET /api/auth/me`
- request-scoped context switch: `POST /api/access/context/activate`
- user account admin: `/api/access/users`
- membership admin: `/api/access/users/{userId}/memberships`
- organization admin: `/api/access/organizations`
- tenant directory: `GET /api/access/tenants`
- tenant market admin: `/api/access/tenants/{tenantId}/markets`

## Important backend expectations
- frontend should treat membership APIs as the main access-management surface
- frontend should treat organization administration as part of the core access shell
- frontend should remove portfolio, operations and reports menus that depended on removed backend routes
- frontend should not call `/api/portfolio/**`, `/api/operations/**` or `/api/reports/**`
- frontend should send `X-Access-Context` when operating under a request-scoped selected membership
