# Changelog

## 2026-03-25
- hard cut completed for the legacy `/api/users` surface; the supported administrative route is `/api/access/users`
- removed legacy users deprecation flags, deprecation headers and operational adoption-report endpoints
- removed authorization fallback based on Cognito tenant claims; active context now resolves strictly from local membership data
- repository reads and user-facing authorization flows were updated to use membership context instead of legacy compatibility hydration
- bootstrap now provisions memberships explicitly for seeded users and internal break-glass users
- operations, reports and authorization now normalize tenant boundary ids consistently during access checks
- Cognito user sync no longer writes legacy tenant custom attributes
- tests were rewritten to validate the final membership-first backend contract
- physical schema cleanup of old `app_user` access columns is still pending in a follow-up migration

## 2026-03-24
- introduced explicit tenant, market and membership structures
- added active context activation and market administration endpoints
