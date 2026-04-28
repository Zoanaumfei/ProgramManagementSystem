# RDS Reset Runbook

Use this only in non-production development environments.

Goal:
- wipe all application data from the RDS-backed schema
- recreate the minimal bootstrap
- keep only `vanderson.verza@gmail.com` as the initial internal admin user
- preserve baseline catalogs/templates required by the application, such as project frameworks and project templates

Preferred path:
1. Confirm the target is the development RDS instance.
2. Make sure no one else is using the environment.
3. Run `scripts/clear-mvp-platform-data.ps1 -ConfirmToken RESET_MVP_PLATFORM_DATA`.
4. Wait for the application to start, execute the maintenance reset and exit.
5. Re-run your normal app startup script afterward if you want the web app online.

Safety guarantees:
- the reset path requires `APP_MAINTENANCE_RESET_ENABLED=true`
- the reset path requires confirmation token `RESET_RDS_SAFE`
- the friendly MVP wrapper requires confirmation token `RESET_MVP_PLATFORM_DATA`
- the reset path does not run unless the application boots successfully against the RDS datasource
- the reset path preserves the schema and only clears application data through the existing bootstrap reset flow

Expected result:
- `internal-core` organization exists
- `vanderson.verza@gmail.com` exists as the internal admin bootstrap user
- no seeded sample users remain when `APP_BOOTSTRAP_SEED_DATA=false`
- project, document, external organization, external user, membership and audit runtime data are cleared
