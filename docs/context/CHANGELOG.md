# Changelog

## 2026-04-28
- added the MVP platform reset flow for the private-RDS dev stack, with `dev-reset.cmd` as the easy operator entry point and an ECS one-off task wrapper for running the reset inside the VPC
- extended the maintenance reset so it clears project/document runtime data, document S3 objects and non-baseline project-management catalogs while preserving the internal admin, Core Oryzem and baseline project frameworks/templates/structure levels
- added scoped IAM helper scripts for allowing `oryzem_admin` to run the ECS reset task without broader IAM administration permissions
- validated the reset flow end-to-end against ECS task definition `program-management-system:71`, with the reset task exiting `0` and the public backend ping healthy afterward

## 2026-04-26
- officialized runtime project structure node endpoints in `docs/context/API_CONTRACT.md`, including tree read, node create/update/move, request payloads and response fields
- clarified that `StructureLevel` is template metadata while `ProjectStructureNode` is the concrete runtime item, such as a `Parts` node under the root `Project` node
- updated `docs/context/FRONTEND_BACKEND_ALIGNMENT.md` and `docs/context/PROJECT_CONTEXT.md` with the frontend handoff expectations for structure-tree UI, node creation and `structureNodeId` scoped project reads
- tracked the remaining frontend implementation work for runtime project structure nodes in `docs/context/OPEN_GAPS.md`

## 2026-04-05
- updated `docs/context/OPEN_GAPS.md` to close the remaining Frente A high-priority items after dev/prod validation and runbook execution evidence
- refreshed `docs/context/PROJECT_CONTEXT.md` to record that the operational dashboard is active in dev and prod and that structured `401`/`403` validation is closed end-to-end
- refreshed `docs/context/FRONTEND_BACKEND_ALIGNMENT.md` so the operational validation section reflects the current dev/prod status and keeps alert-threshold end-to-end evidence as the remaining follow-up

## 2026-04-01
- hardened `users` so create now fails closed when the initial membership cannot be provisioned, with compensating identity cleanup when possible
- changed orphan users from a tolerated lifecycle state into an explicit inconsistency/error condition with stable business codes for API consumers
- corrected organization subtree purge audit attribution so `targetTenantId` records the real tenant instead of the organization id
- aligned organization relationship authorization with semantic actions for create, update and inactivate flows
- refreshed `docs/context/PROJECT_CONTEXT.md` to separate repository-verified backend status from frontend integration tracking
- refreshed `docs/context/FRONTEND_BACKEND_ALIGNMENT.md` so payload examples match the current backend requests and responses
- refreshed `docs/context/API_CONTRACT.md` with the structured error payload contract and the current tenant-directory summary model
- refreshed `docs/context/DECISIONS.md` to reflect that standard user creation now provisions the first membership and `bootstrap-membership` is the exception path
- rewrote `docs/organization-hierarchy.md` around the current relationship-based organization model and split backend versus frontend implications
- updated `docs/context/OPEN_GAPS.md` to close the implemented structured `401`/`403` payload gap and reclassify the remaining work by current priority

## 2026-03-31
- moved organization-level `code` out of the canonical organization model and into relationship-local metadata as `localOrganizationCode`
- added Flyway `V15__move_organization_code_to_relationship_local_metadata.sql` to migrate existing organization codes into `organization_relationship.local_organization_code` and drop `organization.code`
- added `PUT /api/access/organizations/{organizationId}/relationships/{relationshipId}` for updating relationship-local organization codes
- changed `POST /api/access/organizations` to require only `name` and `cnpj`, with optional `localOrganizationCode` applied to the auto-created relationship for external admins
- changed `POST /api/access/users` to provision the first membership in the same request via `organizationId`, optional `marketId` and `roles`
- updated the frontend organization workspace to remove organization-level `code`, show/edit `localOrganizationCode` on relationship flows, and stop rendering a derived `Caminho relacional` breadcrumb that implied a single canonical path
- updated the frontend users workspace so the orphan-user view is explicitly treated as legacy/inconsistency cleanup rather than the normal onboarding path
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
- hardened the active core with tenant-scoped rate limits, tier-based quotas and stricter tenant isolation
- added fail-closed access-context validation so invalid `X-Access-Context` hints no longer fall back silently
- fixed organization-scoped user listing so organization filters remain subtree-scoped instead of leaking full-tenant data
- added audited lifecycle/offboarding behavior for memberships and organization subtrees
- added tenant tier, lifecycle, retention and export metadata for the active core
- updated deployment and validation scripts to match the current public API and authentication flows
- added cross-tenant incident runbook and refreshed the core architecture and contract documentation
- invalid `X-Access-Context` values now fail with `400` instead of default-context fallback
- quota exhaustion now returns `409 Conflict` for child organizations, markets and active memberships
- tenant rate limiting can return `429 Too Many Requests` on authenticated core routes

## 2026-03-25
- completed the structural cleanup that makes `app_user` identity-only in domain and persistence
- added Flyway `V10__remove_legacy_app_user_access_columns.sql` to drop access columns from `app_user`
- changed `POST /api/access/users` so create/update payloads now manage only user lifecycle fields (`displayName`, `email`)
- added `POST /api/access/users/{userId}/bootstrap-membership` as the explicit first-membership onboarding flow
- added regression coverage to block reintroduction of legacy access columns into `app_user`
- the standard onboarding path was refined later on 2026-03-31 so `POST /api/access/users` now provisions the first membership in the same request, while `/bootstrap-membership` remains for lifecycle-only recovery cases

## 2026-03-24
- introduced explicit tenant, market and membership structures
- added active context activation and market administration endpoints
