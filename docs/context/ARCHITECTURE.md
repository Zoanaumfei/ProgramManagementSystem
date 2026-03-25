# Architecture

## Product direction
Oryzem is a SaaS platform for PMEs with multi-tenant and multi-market operation, simple collaboration between customer and supplier organizations, and shared visibility over delivery risk and execution.

## Core access model
The backend now operates with this access chain:

`User -> Membership -> Tenant / Organization / Market -> Role -> Permission`

Definitions:
- `User`: global identity record
- `Tenant`: SaaS boundary and isolation root
- `Organization`: hierarchical business structure inside a tenant
- `Market`: commercial or operational dimension inside the tenant
- `Membership`: contextual access binding for a user
- `Role`: contextual role granted inside a membership
- `Permission`: atomic capability derived from membership roles

## Runtime rules
- Cognito authenticates the identity.
- The application resolves authorization from local memberships.
- `AuthenticatedUser` is always built from `ResolvedMembershipContext`.
- `X-Access-Context` selects the active membership per request.
- `/api/auth/me` exposes the active context already resolved.

## User model boundary
`app_user` is no longer treated as the source of truth for tenant, organization, market, role or tenant type during authorization.
Membership data drives:
- active tenant
- active organization
- active market
- effective roles
- effective permissions

## Administrative surfaces
- `/api/access/users` manages user accounts and the default access assignment used in the admin flow.
- `/api/access/users/{userId}/memberships` manages contextual access explicitly.
- `/api/access/tenants/{tenantId}/markets` manages tenant markets.

## Removed legacy concerns
Removed from the backend:
- legacy `/api/users` route family
- deprecation flags for legacy users flow
- legacy adoption telemetry/report endpoints
- legacy deprecation headers
- auth fallback based on Cognito tenant claims

## Current shape
- `/api/access/users` handles user-account lifecycle and bootstrap of the initial access assignment.
- `/api/access/users/{userId}/memberships` is the authoritative access-management surface.
- authorization and active context resolution do not read legacy `/api/users` flows or Cognito tenant claims.

## Pending cleanup
- physical schema cleanup is still pending for `app_user.role`, `app_user.tenant_id` and `app_user.tenant_type`
- `/api/access/users` still mixes identity lifecycle with default-membership bootstrap in one contract
- some persisted operational records may still contain historical organization-root tenant ids and should be normalized by data migration
