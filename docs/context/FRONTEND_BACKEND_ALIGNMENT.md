# Frontend Backend Alignment

This repository now exposes only the membership-first backend contract.

## Expected frontend integration points
- current user and active context: `GET /api/auth/me`
- request-scoped context switch: `POST /api/access/context/activate`
- user account admin: `/api/access/users`
- membership admin: `/api/access/users/{userId}/memberships`
- tenant market admin: `/api/access/tenants/{tenantId}/markets`

## Important backend expectations
- frontend should treat membership APIs as the main access-management surface
- frontend should send `X-Access-Context` when operating under a request-scoped selected membership
