# Decisions

## ADR-001 Membership is the authorization anchor
Decision:
- membership is the runtime source of contextual authorization

Why:
- one user can operate across tenants, organizations and markets
- SaaS isolation must live above organization hierarchy

## ADR-002 Cognito is authentication, not application authorization
Decision:
- Cognito authenticates identity only
- application authorization is resolved from local membership and role-permission data

Why:
- tenant and role context changes faster than identity shape
- local authorization keeps the product independent from Cognito group topology

## ADR-003 app_user is identity-only and standard onboarding provisions the first membership immediately
Decision:
- `app_user` stores only identity and lifecycle data
- legacy `app_user.role`, `app_user.tenant_id` and `app_user.tenant_type` are removed from the physical schema
- standard user creation now provisions the first membership inside `POST /api/access/users`
- `POST /api/access/users/{userId}/bootstrap-membership` remains available only for lifecycle-only users that still need a first membership assigned later

Why:
- runtime authorization already belongs to membership, so keeping access snapshots in `app_user` creates duplicate truth
- provisioning the first membership during standard user creation reduces failed first-login scenarios and removes an unnecessary admin step from the common path
- removing the columns before production reduces the chance of accidental fallback logic returning later
