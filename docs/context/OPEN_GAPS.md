# Open Gaps

## Closed high priority gaps
- tenant rate limiting now uses a shared store abstraction with Redis as the production default and local mode only for dev/test
- a controlled backoffice/API flow now exists to change `tenant.service_tier` with authorization and audit trail
- an operator-facing organization export surface now exists for offboarded tenants with audited start/complete transitions

## Medium priority
- expose richer tenant directory data if the client needs more than the current tenant summary response
- add explicit regression coverage for removed legacy route families returning missing-route behavior from a consumer perspective
- tighten operational dashboards around new offboarding, quota and rate-limit audit signals

## Low priority
- add a dedicated read model for user listing if the product later wants to separate identity administration from access administration in the UI
- consider splitting tenant internals into finer `api/application/domain/infrastructure` packages only when product work creates real maintenance pressure
