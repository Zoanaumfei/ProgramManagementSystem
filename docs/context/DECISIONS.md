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

## ADR-003 Legacy users surface removed before production
Decision:
- `/api/users` and its coexistence machinery were removed before go-live

Why:
- system is not yet in production
- reducing conceptual duplication now is cheaper than a long coexistence window later

## ADR-004 Membership remains the access source of truth even while `/api/access/users` still bootstraps the first assignment
Decision:
- membership remains the only supported runtime source of tenant, organization, market, role and permission context
- `/api/access/users` may still bootstrap the first membership assignment, but it does not define runtime authorization truth

Why:
- it lets the admin flow stay small during the transition to a pure `user_account` contract
- it keeps the hard cut on `/api/users` while preserving a practical onboarding surface

## ADR-005 Organization remains core while portfolio is frozen
Decision:
- `organization` remains part of the active product core together with `user`, `membership`, `tenant` and `market`
- all `/api/portfolio/**` routes are temporarily disabled instead of being physically removed
- organization administration moves to `/api/access/organizations`
- portfolio runtime data is reset by migration, but the schema is preserved for a future restart

Why:
- the first release needs a smaller, more stable surface centered on access and organization management
- preserving the schema lowers the cost of bringing portfolio back later
- disabling routes is safer than partially deleting backend structure during an active domain reset

## ADR-006 app_user is identity-only and first membership bootstrap is explicit
Decision:
- `app_user` stores only identity and lifecycle data
- legacy `app_user.role`, `app_user.tenant_id` and `app_user.tenant_type` are removed from the physical schema
- `/api/access/users` remains the lifecycle surface
- first membership assignment happens through `/api/access/users/{userId}/bootstrap-membership`

Why:
- runtime authorization already belongs to membership, so keeping access snapshots in `app_user` creates duplicate truth
- splitting bootstrap from lifecycle makes the admin contract easier to reason about and safer to evolve
- removing the columns before production reduces the chance of accidental fallback logic returning later
