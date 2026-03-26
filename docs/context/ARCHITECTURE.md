# Architecture

## Product direction
Oryzem currently ships a core SaaS administration surface centered on user identity, organization hierarchy and membership-based access control.

Active backend scope:
- user lifecycle
- membership-based access control
- tenant and market administration
- organization hierarchy management
- authentication, authorization and audit support for the core
- tenant isolation, quotas and operational rate limiting

Removed from runtime:
- portfolio
- program
- project
- product, item and deliverable execution
- operations
- reports
- portfolio document storage

## Core access model
The backend operates with this access chain:

`User -> Membership -> Tenant / Organization / Market -> Role -> Permission`

Definitions:
- `User`: global identity and lifecycle record
- `Tenant`: SaaS isolation boundary
- `Organization`: hierarchical business structure inside a tenant
- `Market`: optional commercial or regional slice inside a tenant
- `Membership`: contextual binding between a user and a tenant scope
- `Role`: contextual role granted inside a membership
- `Permission`: capability materialized from membership roles

## Runtime rules
- Cognito authenticates identity.
- The application resolves authorization from local memberships.
- `AuthenticatedUser` is always built from `ResolvedMembershipContext`.
- `X-Access-Context` selects the active membership per request.
- invalid `X-Access-Context` hints fail closed instead of silently falling back to another tenant context.
- `/api/auth/me` exposes the resolved active context.
- authenticated core routes are rate limited per tenant.

## Isolation and ownership
- cross-tenant authorization decisions are evaluated from the actor membership and the resource tenant boundary.
- organization-scoped user listing is filtered by organization subtree, never by raw tenant-wide fallback when a narrower organization filter is requested.
- concrete tenant and membership repositories stay encapsulated in `platform.access` behind application services and ports.
- support cross-tenant actions require explicit override + audit-friendly justification where applicable.

## Lifecycle and offboarding
Organizations and memberships now have internal lifecycle state beyond the public `ACTIVE/INACTIVE` contract.

Organization lifecycle:
- `ACTIVE`
- `OFFBOARDING`
- `OFFBOARDED`
- `PURGED`

Membership lifecycle:
- `ACTIVE`
- `REVOKED`
- `OFFBOARDED`

Operational behavior:
- deleting an organization triggers offboarding for the whole subtree.
- offboarding revokes memberships, disables users that lose their last active membership and records audit events.
- offboarded organizations retain a controlled retention deadline and an internal export-ready marker.
- purge remains an explicit support-only destructive step.

## Enterprise controls
- rate limits are enforced per tenant tier.
- quotas are enforced for child organizations, tenant markets and active memberships.
- tenant tier is stored on `tenant.service_tier`.
- current tiers are `INTERNAL`, `STANDARD` and `ENTERPRISE`.
- export is defined as an audited organization-scoped extraction workflow prepared during offboarding, not as a separate public API yet.

## Administrative surfaces
- `/api/auth/me`
- `/api/access/users`
- `/api/access/users/{userId}/bootstrap-membership`
- `/api/access/users/{userId}/memberships`
- `/api/access/organizations`
- `/api/access/tenants`
- `/api/access/tenants/{tenantId}/markets`

## User model boundary
`app_user` is identity-and-lifecycle data only.

It is not the source of truth for tenant, organization, market, role or tenant type during authorization.
Membership data drives:
- active tenant
- active organization
- active market
- effective roles
- effective permissions

## Schema boundary
Authoritative schema after the hardening:
- `organization`
- `tenant`
- `tenant_market`
- `user_membership`
- `membership_role`
- `app_role`
- `app_permission`
- `role_permission`
- `app_user`
- `audit_log`

Important migrations:
- `V10__remove_legacy_app_user_access_columns.sql` removes `app_user.role`, `app_user.tenant_id` and `app_user.tenant_type`
- `V11__remove_dormant_domain_surfaces.sql` drops dormant portfolio/program/project/operations tables and deletes obsolete permission codes
- `V12__enterprise_hardening_core_lifecycle.sql` adds tenant tier, lifecycle state, retention and export metadata for the active core

## Removed legacy concerns
Removed from the backend:
- legacy `/api/users` route family
- legacy portfolio/program/project runtime routes
- legacy operations and reports runtime routes
- temporary portfolio freeze controller and `503` response path
- document-storage wiring dedicated to the removed portfolio runtime
- authorization fallback based on Cognito tenant claims
