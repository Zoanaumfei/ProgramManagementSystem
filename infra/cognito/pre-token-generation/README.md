# Cognito Pre Token Generation

This Lambda now injects only runtime claims that remain useful for the active core:
- `custom:user_status`
- `user_status`
- `username`
- `email`

Important notes:
- tenant, organization and role resolution no longer depend on Cognito tenant claims
- the backend resolves active tenant context from local membership data only
- keeping `username` and `email` in the access token still helps first-login reconciliation for invited users

## Files
- `infra/cognito/pre-token-generation/index.mjs`
- `infra/cognito/pre-token-generation/deploy.ps1`

## Runtime used in the current environment
- `nodejs20.x`

## Operational validation
1. Deploy the Lambda.
2. Force a new login so fresh tokens are issued.
3. Verify the access token contains `username`, `email` when available and `user_status`.
4. Verify `/api/auth/me` resolves `membershipId`, `activeTenantId` and `activeOrganizationId` from backend membership data.

## Important
Do not rebuild authorization around Cognito-side tenant claims. Tenant isolation is enforced by local memberships, organization boundaries, quotas, audit rules and tenant-scoped rate limiting.
