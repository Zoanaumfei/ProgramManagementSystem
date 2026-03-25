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
