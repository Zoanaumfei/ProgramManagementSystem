# Architecture

## Product direction
Oryzem currently ships a core SaaS administration surface centered on user identity, organization relationship networks and membership-based access control.

Active backend scope:
- user lifecycle
- membership-based access control
- tenant and market administration
- organization relationship management
- authentication, authorization and audit support for the core
- tenant isolation, quotas and operational rate limiting

## Core access model
The backend operates with this access chain:

`User -> Membership -> Tenant / Organization / Market -> Role -> Permission`

Definitions:
- `User`: global identity and lifecycle record
- `Tenant`: SaaS isolation boundary
- `Organization`: canonical business entity inside a tenant, identified publicly by `name + cnpj`
- `OrganizationRelationship`: directed customer/supplier or partner edge that carries contextual metadata such as `localOrganizationCode`
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
- structured JSON error responses for security and exception handling include `timestamp`, `status`, `error`, `message`, `path` and `correlationId`.

## Isolation and ownership
- cross-tenant authorization decisions are evaluated from the actor membership and the resource tenant boundary.
- organization-scoped user listing is filtered by the visible `CUSTOMER_SUPPLIER` relationship graph, never by raw tenant-wide fallback when a narrower organization filter is requested.
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
- user purge is an explicit destructive step available to internal `ADMIN` and `SUPPORT` actors under the audited authorization flow.
- organization subtree purge still requires the support-style explicit confirmation path, and now clears relationship edges that reference the subtree before deleting organizations.
- relationship inactivation is historical only; physical cleanup of relationship rows happens during explicit subtree purge.

## Enterprise controls
- rate limits are enforced per tenant tier through a `TenantRateLimitCounterStore` abstraction.
- production uses Redis-backed fixed-window counters so rate limiting remains consistent across instances.
- local counter mode is available only for development and tests through `app.multitenancy.rate-limit.store=local`.
- the ECS test deployment currently uses `local` counter mode until a shared Redis endpoint is provisioned for that environment.
- quotas are enforced for child organizations, tenant markets and active memberships.
- tenant tier is stored on `tenant.service_tier`.
- current tiers are `INTERNAL`, `STANDARD` and `ENTERPRISE`.
- tenant tier changes flow through an audited backoffice/API endpoint and immediately affect quota and rate-limit policy resolution.
- export is defined as an audited organization-scoped extraction workflow prepared during offboarding and exposed through an operator-facing `/api/access/organizations/{organizationId}/exports` surface.

## Administrative surfaces
- `/api/auth/me`
- `/api/access/users`
- `/api/access/users/orphans`
- `/api/access/users/{userId}/bootstrap-membership`
- `/api/access/users/{userId}/memberships`
- `/api/access/organizations`
- `/api/access/organizations/{organizationId}/relationships`
- `/api/access/organizations/{organizationId}/purge-subtree`
- `/api/access/organizations/{organizationId}/exports`
- `/api/access/tenants`
- `/api/access/tenants/{tenantId}/service-tier`
- `/api/access/tenants/{tenantId}/markets`

## User model boundary
`app_user` is identity-and-lifecycle data only.

A valid managed user is still expected to have at least one membership record.

It is not the source of truth for tenant, organization, market, role or tenant type during authorization.
Membership data drives:
- active tenant
- active organization
- active market
- effective roles
- effective permissions

User create now also requires the initial membership context in the same request:
- `organizationId`
- optional `marketId`
- non-empty `roles`

`POST /api/access/users/{userId}/bootstrap-membership` is now an exceptional repair path for inconsistent records, not the standard onboarding path.

If the initial membership cannot be provisioned during `POST /api/access/users`, the create flow must fail atomically and the partially provisioned identity must be compensated when possible.

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
- `V12__enterprise_hardening_core_lifecycle.sql` adds tenant tier, lifecycle state, retention and export metadata for the active core
- `V13__introduce_organization_relationships_and_reset_access_core.sql` introduces explicit organization relationship storage for the active core
- `V14__replace_organization_hierarchy_with_cnpj_identity.sql` removes legacy stored hierarchy metadata in favor of canonical tenant-scoped `cnpj`
- `V15__move_organization_code_to_relationship_local_metadata.sql` migrates organization-level codes into `organization_relationship.local_organization_code`
