# Project Context

Oryzem is being positioned as a SaaS platform for PMEs that need simple multi-tenant and multi-market administration across customer and supplier organizations.

## Active product scope
- user lifecycle
- organization relationship network
- membership-based access control
- tenant and market administration
- authentication, authorization and audit support around that core

## Backend status
The backend status below was cross-checked against the code currently present in this repository.

- the active backend surface is centered on `/api/auth/me`, `/api/access/users`, `/api/access/users/{userId}/memberships`, `/api/access/organizations`, `/api/access/organizations/{organizationId}/relationships`, `/api/access/tenants` and related operational endpoints
- user onboarding now provisions the first membership during `POST /api/access/users`, so the standard invite flow no longer depends on a later bootstrap-membership step
- `GET /api/access/users/orphans` exists only as an inconsistency discovery surface for unexpected legacy/test data; orphan users are no longer treated as a valid lifecycle state
- internal break-glass administration resolves from local membership roles `ADMIN`, `SUPPORT` and `AUDITOR`, matching the Cognito bootstrap groups used for the internal global user
- user purge is now allowed for internal `ADMIN` and `SUPPORT` actors, but only for inactive users whose Cognito identity is already absent and when the authorization/audit requirements are satisfied
- the backend treats `cnpj` as the canonical organization identity inside a tenant
- organization creation no longer depends on stored hierarchy fields such as `parentOrganizationId`, `customerOrganizationId` or `hierarchyLevel`
- organization visibility is driven by explicit directed relationships, with `CUSTOMER_SUPPLIER` and `PARTNER` edges
- relationship-local organization coding now lives on `localOrganizationCode`, including create/update validation and duplicate protection per source organization
- external organization create now follows a create-or-reuse flow by `tenant + cnpj`, automatically creating or reactivating the relevant `CUSTOMER_SUPPLIER` relationship
- organization subtree delete performs offboarding first, and the explicit purge flow now clears relationship rows that reference the subtree, including inactive ones, before physical deletion
- tenant rate limiting uses a shared `TenantRateLimitCounterStore` abstraction, with Redis as the production default and local mode reserved for dev/test
- the ECS test deployment still runs the rate-limit store in local mode until a shared Redis dependency is provisioned for that environment
- tenant service tier changes flow through an audited `PATCH /api/access/tenants/{tenantId}/service-tier` endpoint and immediately affect quota and rate-limit policy resolution
- offboarded organizations now expose an audited export workflow through `/api/access/organizations/{organizationId}/exports`
- structured `401`/`403` payloads are implemented in the backend with `timestamp`, `status`, `error`, `message`, `path` and `correlationId`
- legacy runtime families `/api/portfolio/**`, `/api/operations/**`, `/api/reports/**` and `/api/users/**` are no longer part of the supported backend surface

## Frontend integration status
The frontend source tree is not the active implementation focus of this repository today, so the notes below should be treated as integration guidance plus tracked product status rather than fully repository-verified UI code.

- the active frontend direction remains the access-first workspace flow centered on `/workspace`, `/workspace/users`, `/workspace/organizations`, `/workspace/markets` and `/workspace/session`
- the users experience should consume `/api/access/users` and membership APIs as the primary access-management surface
- the users workspace may expose a dedicated `Usuarios sem membership` diagnostic view backed by `/api/access/users/orphans`, explicitly positioned as data-repair tooling rather than the normal onboarding path
- user creation in the frontend should require `organizationId`, optional `marketId` and non-empty `roles` so the first membership is provisioned in the same request
- the frontend users workspace now surfaces orphan-user and missing-active-membership failures as operational inconsistency states, not as generic request errors, and includes `correlationId` in the administrative error copy when the backend provides it
- the memberships workspace copy now treats `bootstrap-membership` as an exceptional repair flow only; normal onboarding messaging points to `POST /api/access/users` as the standard path
- organization management in the frontend should follow the relationship-first model and treat `localOrganizationCode` as relationship metadata, not organization identity
- frontend flows should stop depending on legacy hierarchy fields and should not imply a single canonical relationship breadcrumb
- frontend error handling should surface backend-provided `401`/`403` messages and `correlationId` values consistently
- the frontend admin rollout for structured `401`/`403` is now implemented in the active frontend workspace: `/workspace/users`, `/workspace/organizations` and `/workspace/session` preserve backend `message`, `path` and `correlationId` instead of collapsing those cases into generic errors
- frontend regression coverage now includes structured security-error handling for `/api/auth/me`, `/api/access/users` and `/api/access/organizations`, including visible `correlationId` assertions in the admin UI
- frontend environment resolution now auto-upgrades an insecure `http` API base URL to `https` whenever the app itself is running under an `https` origin, reducing published mixed-content failures during auth/login
- the published backend environment was revalidated on `2026-04-01`: `GET /api/access/users` now resolves through the secured backend route and returns structured `401 Unauthorized` for anonymous traffic instead of the previously reported static-resource-style `404`

## Core themes
- user and organization administration as the release anchor
- contextual access control by tenant, organization and market
- low-friction administration suitable for smaller and medium-sized businesses
- clear separation between identity data and runtime authorization context

## Out of scope for the active product
- portfolio
- program
- project
- product, item and deliverable execution
- operations
- reports
- portfolio-specific document storage

Those capabilities are not frozen in place anymore. They were removed from runtime and schema support so the codebase matches the active product boundary.
