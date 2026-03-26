# Architecture

## Product direction
Oryzem is a SaaS platform for PMEs with multi-tenant and multi-market operation, simple collaboration between customer and supplier organizations, and shared visibility over delivery risk and execution.

The current implementation focus is temporarily constrained to the User + Organization core. Portfolio execution capabilities stay modeled in the backend, but their active routes and data are frozen until a later reactivation phase.

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
`app_user` is now an identity-and-lifecycle record only.

It is no longer treated as the source of truth for tenant, organization, market, role or tenant type during authorization.
Membership data drives:
- active tenant
- active organization
- active market
- effective roles
- effective permissions

## Administrative surfaces
- `/api/access/users` manages user-account lifecycle only.
- `/api/access/users/{userId}/bootstrap-membership` bootstraps the first membership for a newly created user.
- `/api/access/users/{userId}/memberships` manages contextual access explicitly.
- `/api/access/organizations` manages the organization hierarchy as a core surface, independent from portfolio execution.
- `/api/access/tenants/{tenantId}/markets` manages tenant markets.

## Portfolio freeze
- all `/api/portfolio/**` routes are intentionally disabled with `503 Service Unavailable`
- the freeze message directs clients to keep organization management on `/api/access/organizations`
- Flyway migration `V9__freeze_portfolio_and_reset_data.sql` removes portfolio runtime data without dropping schema or touching the membership-first access core
- `organization`, `tenant`, `tenant_market`, `user_membership`, `membership_role`, `app_role`, `app_permission` and `role_permission` remain authoritative and preserved

## Removed legacy concerns
Removed from the backend:
- legacy `/api/users` route family
- deprecation flags for legacy users flow
- legacy adoption telemetry/report endpoints
- legacy deprecation headers
- auth fallback based on Cognito tenant claims

## Current shape
- `/api/access/users` handles user-account lifecycle only.
- `/api/access/users/{userId}/bootstrap-membership` is the explicit first-membership onboarding step.
- `/api/access/users/{userId}/memberships` is the authoritative access-management surface.
- authorization and active context resolution do not read legacy `/api/users` flows or Cognito tenant claims.

## Schema boundary
- Flyway `V10__remove_legacy_app_user_access_columns.sql` removes `app_user.role`, `app_user.tenant_id` and `app_user.tenant_type`
- this is a breaking schema change for any direct SQL, ETL or local tooling that still reads those legacy columns
- all authorization paths must read tenant, organization, market and role context from `user_membership` plus `membership_role`

## Pending cleanup
- some persisted operational records may still contain historical organization-root tenant ids and should be normalized by data migration
- portfolio services, entities and storage integrations are intentionally kept compiled but dormant during the freeze so future retomada can happen incrementally
