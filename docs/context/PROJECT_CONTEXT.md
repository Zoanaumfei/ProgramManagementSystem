# Project Context

Oryzem is being positioned as a SaaS platform for PMEs that need simple multi-tenant and multi-market administration across customer and supplier organizations.

The current product scope is the core administration slice:
- user lifecycle
- organization hierarchy
- membership-based access control
- tenant and market administration
- authentication, authorization and audit support around that core

Core themes:
- user and organization administration as the release anchor
- contextual access control by tenant, organization and market
- low-friction administration suitable for smaller and medium-sized businesses
- clear separation between identity data and runtime authorization context

Current implementation status:
- the active frontend shell is aligned to the access-first workspace flow
- primary navigation now centers `/workspace`, `/workspace/users`, `/workspace/organizations`, `/workspace/markets` and `/workspace/session`
- the current frontend build, lint and test suite were verified on 2026-03-28 after the access migration
- the legacy `src/features/portfolio` frontend subtree has been removed from the codebase
- the users page now consumes only the user and membership APIs already implemented in the backend contract
- the users list is scoped to the active organization in the current session, while create/update enforce global email uniqueness across the local user repository
- the user purge action is intentionally visible only to `SUPPORT` users and only for accounts already marked `INACTIVE` with the Cognito identity absent
- internal `ADMIN` sessions now list users without organization scoping in the frontend, while external sessions remain scoped by active organization
- the membership administration form now lets `ADMIN` sessions choose the destination organization from the visible organization catalog instead of hard-coding the active session organization
- the membership administration form labels the target organization explicitly as `Organization destino` to avoid confusing it with the session context
- the users workspace now exposes a dedicated `Usuários sem membership` section backed by `/api/access/users/orphans` when the backend read model is available, and clicking an orphan user jumps to the membership bootstrap flow
- the frontend error helpers now surface structured `401`/`403` payload messages and correlation ids when the backend provides them
- membership cards in the users workspace now use an ID-card style layout to surface tenant, organization, market, roles and membership metadata more visually
- membership cards and user membership snapshots now render the organization hierarchy tree as a breadcrumb such as `Volkswagen -> Gestamp -> Delga` when the visible organization catalog contains the parent chain
- the frontend now consumes the directed relationship model for organizations, including relationship creation/inactivation in the organization workspace and hierarchy breadcrumb rendering from active `CUSTOMER_SUPPLIER` edges
- organization creation in the frontend no longer asks for `parentOrganizationId`; the old tree fields are now derived from relationships and the organization cards treat direct partners as view-only
- the backend now exposes `GET /api/access/users/orphans` and organization relationship endpoints for bootstrap discovery and explicit relationship management
- next access-management step under consideration: standardize 401/403 error payloads with `timestamp`, `status`, `error`, `message`, `path` and `correlationId` so the frontend can show the real refusal reason
- the published backend environment still needs validation for `GET /api/access/users`, which returned a static-resource-style 404 during frontend testing

Out of scope for the active product:
- portfolio
- program
- project
- product, item and deliverable execution
- operations
- reports
- portfolio-specific document storage

Those capabilities are not frozen in place anymore. They were removed from runtime and schema support so the codebase matches the active product boundary.
