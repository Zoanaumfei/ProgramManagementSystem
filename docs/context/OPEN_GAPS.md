# Open Gaps

## Closed high priority gaps
- tenant rate limiting now uses a shared store abstraction with Redis as the production default and local mode only for dev/test
- a controlled backoffice/API flow now exists to change `tenant.service_tier` with authorization and audit trail
- an operator-facing organization export surface now exists for offboarded tenants with audited start/complete transitions
- organization relationship management now exists with explicit `CUSTOMER_SUPPLIER` and `PARTNER` edges
- relationship-local organization coding now exists through `localOrganizationCode`, including create/update validation and frontend support
- organization-scoped orphan discovery now exists through `GET /api/access/users/orphans`
- organization subtree purge now clears relationship rows that reference the subtree, including inactive ones, before physical deletion
- user creation now fails closed when the initial membership cannot be provisioned, and orphan users are treated as explicit inconsistency errors instead of normal lifecycle state
- structured `401`/`403` auth payloads now ship with `timestamp`, `status`, `error`, `message`, `path` and `correlationId`, with backend coverage in the security and exception-handler tests
- the published backend environment now resolves `GET /api/access/users` through the secured controller path; anonymous validation on `2026-04-01` returned structured `401 Unauthorized` instead of a static-resource `404`
- the current ECS test environment intentionally runs tenant rate limiting in local mode until a shared Redis dependency is added for that stack
- the published environments were validated end-to-end for `/api/auth/me`, `/api/access/users` and `/api/access/organizations`, confirming the structured `401`/`403` contract in dev and prod
- the minimum operational dashboard is now live in the frontend on dev and prod for `429`, `409` quota, offboarding and export requested/completed signals
- the operational reaction runbook has been exercised successfully in dev and prod against the active core flows

## Open high priority gaps
- none currently tracked for Frente A

## Open medium priority gaps
- exercise the automatic alert-threshold flows end-to-end and capture explicit evidence for the already-implemented `429` spike, quota spike and export-backlog rules
- expose richer tenant directory data if the client needs more than the current tenant summary response
- add explicit regression coverage for removed legacy route families returning missing-route behavior from a consumer perspective
- confirm whether the frontend should show direct-partner relationships as view-only in the organization and user workspaces, or surface that distinction only through authorization failures
- expose a public project-template discovery endpoint if the frontend should stop relying on known seeded defaults for `APQP`, `VDA_MLA` and `CUSTOM`
- finish the dedicated participant-management UI for project organizations/members on top of the already-available backend endpoints
- implement the project-management operational inbox view proposed for `/workspace/projects/inbox`

## Open low priority gaps
- add a dedicated read model for user listing if the product later wants to separate identity administration from access administration in the UI
- consider splitting tenant internals into finer `api/application/domain/infrastructure` packages only when product work creates real maintenance pressure
