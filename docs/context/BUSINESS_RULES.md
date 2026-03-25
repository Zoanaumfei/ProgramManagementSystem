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
- `POST /api/access/users/{userId}/purge` is an explicit support cleanup flow for inactive users whose Cognito identity is already absent

## Organizations
- organization belongs to a tenant
- organization may belong to a market
- organization hierarchy controls subtree visibility
- organization is not the tenant boundary itself

## Markets
- market belongs to a tenant
- inactive markets cannot be assigned to memberships
- a market cannot be inactivated while referenced by active memberships or organizations
