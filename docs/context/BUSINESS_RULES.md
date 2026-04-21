# Business Rules

## Access
- Every authenticated request must resolve to an active local membership.
- A user may have multiple memberships.
- Only active memberships can be activated.
- One membership may be flagged as default.
- Invalid request-scoped membership selection via `X-Access-Context` must fail closed.

## Users
- user accounts represent global identity and lifecycle state
- membership grants contextual access
- user creation must already include the first valid membership context (`organizationId` + `roles`, and optional `marketId`)
- a managed user without any membership is an invalid state and must be treated as creation inconsistency or corrupted test data
- `POST /api/access/users/{userId}/bootstrap-membership` is reserved for exceptional repair flows only and is not part of standard onboarding
- `DELETE /api/access/users/{userId}` performs logical inactivation
- the internal break-glass user is expected to carry local membership roles `ADMIN`, `SUPPORT` and `AUDITOR`
- `POST /api/access/users/{userId}/purge` is an explicit destructive cleanup flow for inactive users whose Cognito identity is already absent
- internal `ADMIN` and `SUPPORT` actors can execute user purge when the authorization/audit requirements are satisfied
- create user must fail atomically if the initial membership cannot be provisioned
- lifecycle operations that detect an orphan user must fail closed with explicit business errors instead of borrowing the actor context

## Organizations
- organization belongs to a tenant
- organization may belong to a market
- organization identity inside a tenant is defined by canonical `cnpj`
- organization no longer owns a global business `code` in the public model
- the platform uses active `CUSTOMER_SUPPLIER` relationships, not a stored parent/child hierarchy, to control subtree visibility
- one organization may be customer of several suppliers and supplier of several customers
- when an external admin creates an organization, the system must create or reactivate the `CUSTOMER_SUPPLIER` relationship from the actor organization to the target organization
- if another organization with the same `cnpj` already exists in the same tenant, the system must reuse it instead of creating a duplicate
- if `localOrganizationCode` is provided during external organization create, it must be written on that automatic relationship
- `CUSTOMER_SUPPLIER` relationships cannot point to the same organization and cannot introduce cycles
- `PARTNER` relationships are explicit lateral links and do not replace the customer/supplier traversal used for subtree visibility
- relationship-local metadata may include `localOrganizationCode`
- `localOrganizationCode` belongs to the source -> target relationship context and may differ for the same target organization under different source organizations
- if present, `localOrganizationCode` must be unique per source organization
- organization is not the tenant boundary itself
- relationship records may stay persisted as `INACTIVE` for historical purposes
- organization subtree purge must remove any relationship rows that reference the subtree before physically deleting organizations
- organization delete performs offboarding first; export handling lives inside the retention/offboarding workflow

## Markets
- market belongs to a tenant
- inactive markets cannot be assigned to memberships
- a market cannot be inactivated while referenced by active memberships or organizations

## Templates
- project templates and project structure templates have immutable ownership defined by the creator active organization context
- template visibility and use follow the active `CUSTOMER_SUPPLIER` chain transitively from the owner organization to descendant organizations
- only the owner organization can manage templates and template artifacts (update, purge, activate/deactivate, phases, milestones, deliverables, structure levels)
- non-owner organizations in the chain may view and use inherited templates but cannot edit or purge them
- project creation with explicit `templateId` must fail closed when the active organization is not authorized to use the template
- project creation without explicit `templateId` resolves the default template only from the active organization authorized catalog

## Projects
- project listing is tenant-scoped by default
- internal `ADMIN` actors may list projects across all organizations/tenants on the platform
- internal `ADMIN` and `SUPPORT` actors may execute the explicit project purge flow
- project purge requires a non-empty operator reason plus a second explicit confirmation step
- the final purge confirmation requires the exact confirmation text `PURGE PROJECT`
- project purge is a destructive cleanup flow intended for administrative/offboarding scenarios and must remove both persisted project data and linked document/storage artifacts
- project purge orchestration belongs to the `projectmanagement` module; shared/core modules should only provide technical capabilities consumed by that flow

## Operational controls
- tenant rate limits are enforced per tenant tier
- quota exhaustion can block creation of child organizations, tenant markets and active memberships
- tenant service-tier changes must be audited and apply immediately to quota and rate-limit policy resolution
