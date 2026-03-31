# Business Rules

## Access
- Every authenticated request must resolve to an active local membership.
- A user may have multiple memberships.
- Only active memberships can be activated.
- One membership may be flagged as default.

## Users
- user accounts represent global identity and lifecycle state
- membership grants contextual access
- `DELETE /api/access/users/{userId}` performs logical inactivation
- the internal break-glass user is expected to carry local membership roles `ADMIN`, `SUPPORT` and `AUDITOR`
- `POST /api/access/users/{userId}/purge` is an explicit destructive cleanup flow for inactive users whose Cognito identity is already absent
- internal `ADMIN` and `SUPPORT` actors can execute user purge when the authorization/audit requirements are satisfied

## Organizations
- organization belongs to a tenant
- organization may belong to a market
- organization identity inside a tenant is defined by canonical `cnpj`
- the platform uses active `CUSTOMER_SUPPLIER` relationships, not a stored parent/child hierarchy, to control subtree visibility
- one organization may be customer of several suppliers and supplier of several customers
- when an external admin creates an organization, the system must create or reactivate the `CUSTOMER_SUPPLIER` relationship from the actor organization to the target organization
- if another organization with the same `cnpj` already exists in the same tenant, the system must reuse it instead of creating a duplicate
- `CUSTOMER_SUPPLIER` relationships cannot point to the same organization and cannot introduce cycles
- organization is not the tenant boundary itself
- relationship records may stay persisted as `INACTIVE` for historical purposes
- organization subtree purge must remove any relationship rows that reference the subtree before physically deleting organizations

## Markets
- market belongs to a tenant
- inactive markets cannot be assigned to memberships
- a market cannot be inactivated while referenced by active memberships or organizations
