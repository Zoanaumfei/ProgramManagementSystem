# Frontend Backend Alignment

This repository now exposes only the membership-first backend contract.

## Expected frontend integration points
- current user and active context: `GET /api/auth/me`
- request-scoped context switch: `POST /api/access/context/activate`
- user account admin: `/api/access/users`
- membership admin: `/api/access/users/{userId}/memberships`
- organization admin: `/api/access/organizations`
- tenant market admin: `/api/access/tenants/{tenantId}/markets`

## Important backend expectations
- frontend should treat membership APIs as the main access-management surface
- frontend should treat organization administration as part of the core access shell, not as portfolio UI
- frontend should hide portfolio menus and routes for now; backend portfolio calls intentionally return `503 Service Unavailable`
- frontend should send `X-Access-Context` when operating under a request-scoped selected membership
