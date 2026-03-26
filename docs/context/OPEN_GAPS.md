# Open Gaps

## High priority
- move tenant rate limiting from the current in-memory node-local counter to a shared store before multi-instance production scale-out
- add an explicit operator-facing export execution surface if the manual audited export workflow becomes a frequent operational path
- define a controlled backoffice flow to change `tenant.service_tier` without direct database maintenance

## Medium priority
- expose richer tenant directory data if the client needs more than the current tenant summary response
- add explicit regression coverage for removed legacy route families returning missing-route behavior from a consumer perspective
- tighten operational dashboards around new offboarding, quota and rate-limit audit signals

## Low priority
- add a dedicated read model for user listing if the product later wants to separate identity administration from access administration in the UI
- consider splitting tenant internals into finer `api/application/domain/infrastructure` packages only when product work creates real maintenance pressure
