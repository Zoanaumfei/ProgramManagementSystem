# Changelog

## 2026-03-31
- introduced canonical tenant-scoped `cnpj` identity for organizations and removed the legacy parent/customer/hierarchy organization tree fields from the active public contract
- changed external organization creation to create-or-reuse by `tenant + cnpj` and automatically create or reactivate the `CUSTOMER_SUPPLIER` relationship from the actor organization
- added cycle and self-link protection for `CUSTOMER_SUPPLIER` relationship management
- added Flyway `V14__replace_organization_hierarchy_with_cnpj_identity.sql` to replace stored hierarchy metadata with canonical `cnpj` identity
- fixed Flyway `V13__introduce_organization_relationships_and_reset_access_core.sql` so integration tests can run on the H2 test environment
- aligned the bootstrap internal break-glass membership with local roles `ADMIN`, `SUPPORT` and `AUDITOR` so runtime authorization matches the Cognito bootstrap groups
- allowed internal `ADMIN` actors to execute the user purge flow in the authorization matrix
- fixed organization subtree purge so persisted `organization_relationship` rows, including inactive ones, are removed before physical organization deletion
- updated context and contract notes to document the difference between relationship inactivation and purge-time cleanup

## 2026-03-28
- documented the RDS-safe bootstrap reset path for non-production environments
- documented the ECS test-environment rate-limit fallback to local counters while Redis is not provisioned there
- recorded the operational impact of the shared tenant rate-limit store on the `/api/auth/me` smoke path

## 2026-03-27
- added a shared tenant rate-limit counter abstraction with Redis-backed fixed-window counters for production and explicit local mode for dev/test
- added an audited `PATCH /api/access/tenants/{tenantId}/service-tier` backoffice flow to change tenant tier with immediate policy impact
- added audited operator-facing organization export endpoints for offboarded organizations with eligibility checks and staged completion
- updated the core contract, architecture notes and open-gap tracking to reflect the closed high-priority hardening work

## 2026-03-26
- permanently removed portfolio/program/project, operations and reports runtime modules from the application
- deleted dormant document-storage wiring that existed only for the removed portfolio runtime
- removed all temporary `/api/portfolio/**` freeze handling and the legacy `503 Service Unavailable` contract
- renamed tenant-side wiring away from legacy portfolio terminology and kept only core user + organization + access services
- removed obsolete `portfolio.*`, `operations.*` and `reports.*` permission codes from Flyway-managed data
- added Flyway `V11__remove_dormant_domain_surfaces.sql` to drop dormant domain tables and permission data
- removed portfolio document configuration from application profiles and dropped the unused S3 dependency
- updated tests and architecture guardrails to validate the final core-only runtime
- added enterprise hardening for the active core with tenant-scoped rate limits, tier-based quotas and stricter tenant isolation
- added fail-closed access-context validation so invalid `X-Access-Context` hints no longer fall back silently
- fixed organization-scoped user listing so organization filters remain subtree-scoped instead of leaking full-tenant data
- added audited lifecycle/offboarding behavior for memberships and organization subtrees
- added Flyway `V12__enterprise_hardening_core_lifecycle.sql` for tenant tier, lifecycle, retention and export metadata
- cleaned ECS/Cognito helper scripts so core deployment and auth validation no longer depend on removed portfolio document or legacy tenant-claim assumptions
- added cross-tenant incident runbook and updated core architecture/contract documentation

### Breaking changes
- route families removed: `/api/portfolio/**`, `/api/operations/**`, `/api/reports/**`
- temporary freeze behavior removed: callers no longer receive the old portfolio `503` response path
- `OrganizationResponse` no longer contains program summary data
- `OrganizationPurgeResponse` no longer contains purged program/document counters
- `/api/auth/me` and membership permission payloads no longer include `portfolio.*`, `operations.*` or `reports.*`
- schema: dormant tables created by legacy portfolio/program/project and operations modules are dropped by `V11`
- invalid `X-Access-Context` values now fail with `400` instead of default-context fallback
- quota exhaustion now returns `409 Conflict` for child organizations, markets and active memberships
- tenant rate limiting can return `429 Too Many Requests` on authenticated core routes

## 2026-03-25
- hard cut completed for the legacy `/api/users` surface; the supported administrative route is `/api/access/users`
- removed legacy users deprecation flags, deprecation headers and operational adoption-report endpoints
- removed authorization fallback based on Cognito tenant claims; active context now resolves strictly from local membership data
- repository reads and user-facing authorization flows were updated to use membership context instead of legacy compatibility hydration
- bootstrap now provisions memberships explicitly for seeded users and internal break-glass users
- completed the structural cleanup that makes `app_user` identity-only in domain and persistence
- added Flyway `V10__remove_legacy_app_user_access_columns.sql` to drop `app_user.role`, `app_user.tenant_id` and `app_user.tenant_type`
- changed `/api/access/users` so create/update payloads now manage only user lifecycle fields (`displayName`, `email`)
- added `POST /api/access/users/{userId}/bootstrap-membership` as the explicit first-membership onboarding flow
- added regression coverage to block reintroduction of legacy access columns into `app_user`

### Breaking changes
- schema: `app_user.role`, `app_user.tenant_id` and `app_user.tenant_type` were removed
- API: `POST /api/access/users` and `PUT /api/access/users/{userId}` no longer accept role or organization bootstrap fields
- integration contract: authorization must operate exclusively through membership context

## 2026-03-24
- introduced explicit tenant, market and membership structures
- added active context activation and market administration endpoints
