# Open Gaps

## High priority
- create a Flyway migration that removes `app_user.role`, `app_user.tenant_id` and `app_user.tenant_type` after the persistence model is fully shifted to identity-only `app_user` plus `user_membership`
- refactor `ManagedUser` and `UserEntity` so user persistence carries only identity/lifecycle data and no contextual access snapshot
- split `/api/access/users` into a pure user-account contract and a separate explicit bootstrap flow for the first membership assignment

## Medium priority
- backfill and normalize any persisted `operation_record.tenant_id` rows that still use organization-root ids instead of canonical tenant ids
- review remaining bootstrap and Cognito provisioning flows so role resolution always comes from membership at the service boundary, not from temporary account snapshots
- expose richer tenant directory data if the client needs more than the current tenant summary response

## Low priority
- add a dedicated read model for user account listing if the product wants to separate identity administration from access administration in reporting and UI
