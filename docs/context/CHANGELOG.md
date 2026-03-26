# Changelog

## 2026-03-25
- reset portfolio runtime data through Flyway `V9__freeze_portfolio_and_reset_data.sql` while preserving `organization`, `tenant`, `user_membership`, roles and permissions
- moved organization administration from the portfolio namespace to `/api/access/organizations`
- froze all `/api/portfolio/**` routes behind a temporary `503 Service Unavailable` response so clients can hide portfolio menus without breaking the backend build
- documented the temporary product focus on the User + Organization core and captured a future portfolio retomada path
- hard cut completed for the legacy `/api/users` surface; the supported administrative route is `/api/access/users`
- removed legacy users deprecation flags, deprecation headers and operational adoption-report endpoints
- removed authorization fallback based on Cognito tenant claims; active context now resolves strictly from local membership data
- repository reads and user-facing authorization flows were updated to use membership context instead of legacy compatibility hydration
- bootstrap now provisions memberships explicitly for seeded users and internal break-glass users
- operations, reports and authorization now normalize tenant boundary ids consistently during access checks
- Cognito user sync no longer writes legacy tenant custom attributes
- tests were rewritten to validate the final membership-first backend contract
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
