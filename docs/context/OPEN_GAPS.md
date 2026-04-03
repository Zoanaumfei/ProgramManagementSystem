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

## Open high priority gaps
- validate the published environment end-to-end so the frontend consistently receives the structured `401`/`403` payloads already implemented in the backend
- tighten operational dashboards around new offboarding, quota and rate-limit audit signals
- implement the aggregated operational overview endpoint for the frontend dashboard, including tenant drill-down details and alert snapshots

## Open medium priority gaps
- expose richer tenant directory data if the client needs more than the current tenant summary response
- add explicit regression coverage for removed legacy route families returning missing-route behavior from a consumer perspective
- confirm whether the frontend should show direct-partner relationships as view-only in the organization and user workspaces, or surface that distinction only through authorization failures

## Open low priority gaps
- add a dedicated read model for user listing if the product later wants to separate identity administration from access administration in the UI
- consider splitting tenant internals into finer `api/application/domain/infrastructure` packages only when product work creates real maintenance pressure
