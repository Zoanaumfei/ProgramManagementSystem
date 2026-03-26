# Open Gaps

## High priority
- define the reactivation plan for frozen portfolio surfaces, including client unhide criteria, staged endpoint rollout and data reseeding strategy

## Medium priority
- backfill and normalize any persisted `operation_record.tenant_id` rows that still use organization-root ids instead of canonical tenant ids
- harden downstream integrations and scripts so they do not expect removed `app_user.role`, `app_user.tenant_id` or `app_user.tenant_type` columns
- review remaining bootstrap and Cognito provisioning flows so role resolution always comes from membership at the service boundary, not from any temporary compatibility snapshot
- expose richer tenant directory data if the client needs more than the current tenant summary response
- rename legacy `portfolio`-prefixed organization services once the temporary freeze is complete so the codebase language matches the new core boundary

## Low priority
- add a dedicated read model for user account listing if the product wants to separate identity administration from access administration in reporting and UI
- remove dormant portfolio adapters only after the portfolio retomada plan is either implemented or explicitly abandoned
