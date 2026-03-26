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

## ADR-004 app_user is identity-only and first membership bootstrap is explicit
Decision:
- `app_user` stores only identity and lifecycle data
- legacy `app_user.role`, `app_user.tenant_id` and `app_user.tenant_type` are removed from the physical schema
- `/api/access/users` remains the lifecycle surface
- first membership assignment happens through `/api/access/users/{userId}/bootstrap-membership`

Why:
- runtime authorization already belongs to membership, so keeping access snapshots in `app_user` creates duplicate truth
- splitting bootstrap from lifecycle makes the admin contract easier to reason about and safer to evolve
- removing the columns before production reduces the chance of accidental fallback logic returning later

## ADR-005 Portfolio/program/project runtime is removed instead of frozen
Decision:
- the active product boundary is `user + organization + access`
- dormant portfolio/program/project, operations and reports modules are physically removed from runtime
- temporary `/api/portfolio/**` freeze behavior is retired
- legacy schema support for those dormant modules is dropped by migration `V11__remove_dormant_domain_surfaces.sql`

Why:
- keeping dormant code and tables created maintenance cost without supporting the active product
- removing the temporary freeze contract avoids confusing clients about what is actually supported
- a future re-entry of portfolio/project work should be a deliberate new capability, not a hidden dormant runtime
