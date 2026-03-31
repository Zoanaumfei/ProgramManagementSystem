# Open Gaps

## Closed high priority gaps
- tenant rate limiting now uses a shared store abstraction with Redis as the production default and local mode only for dev/test
- a controlled backoffice/API flow now exists to change `tenant.service_tier` with authorization and audit trail
- an operator-facing organization export surface now exists for offboarded tenants with audited start/complete transitions
- organization relationship management now exists with explicit `CUSTOMER_SUPPLIER` and `PARTNER` edges
- organization-scoped orphan discovery now exists through `GET /api/access/users/orphans`
- organization subtree purge now clears relationship rows that reference the subtree, including inactive ones, before physical deletion
- the current ECS test environment intentionally runs tenant rate limiting in local mode until a shared Redis dependency is added for that stack

## Medium priority
- expose richer tenant directory data if the client needs more than the current tenant summary response
- add explicit regression coverage for removed legacy route families returning missing-route behavior from a consumer perspective
- tighten operational dashboards around new offboarding, quota and rate-limit audit signals
- validate and redeploy the published backend environment so `GET /api/access/users` resolves to the user-management controller instead of a static-resource 404
- validate the structured `401`/`403` auth payload rollout end-to-end so the frontend can display the backend refusal reason and correlation id consistently
- confirm whether the frontend should show direct-partner relationships as view-only in the organization and user workspaces, or surface that distinction only through authorization failures

## Low priority
- add a dedicated read model for user listing if the product later wants to separate identity administration from access administration in the UI
- consider splitting tenant internals into finer `api/application/domain/infrastructure` packages only when product work creates real maintenance pressure
