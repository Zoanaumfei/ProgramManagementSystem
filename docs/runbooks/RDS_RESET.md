# RDS Reset Runbook

Use this only in non-production development environments.

Goal:
- wipe all application data from the RDS-backed schema
- recreate the minimal bootstrap
- keep only `vanderson.verza@gmail.com` as the initial internal admin user
- preserve baseline catalogs/templates required by the application, such as project frameworks and project templates
- delete document objects from the configured S3 document storage prefix

Preferred path:
1. Confirm the target is the development RDS instance.
2. Make sure no one else is using the environment.
3. For the current private-RDS dev environment, run `dev-reset.cmd`.
4. Type `RESET` when prompted.
5. Wait for the one-off ECS task to start, execute the maintenance reset and stop with exit code `0`.

Direct script alternatives:
- private RDS / current AWS dev stack: `scripts/clear-mvp-platform-data-ecs.ps1 -ConfirmToken RESET_MVP_PLATFORM_DATA`
- locally reachable RDS only: `scripts/clear-mvp-platform-data.ps1 -ConfirmToken RESET_MVP_PLATFORM_DATA`

Safety guarantees:
- the reset path requires `APP_MAINTENANCE_RESET_ENABLED=true`
- the reset path requires confirmation token `RESET_RDS_SAFE`
- the friendly MVP wrapper requires confirmation token `RESET_MVP_PLATFORM_DATA`
- `dev-reset.cmd` requires the operator to type `RESET` before it forwards to the ECS wrapper
- the ECS wrapper requires `ecs:RunTask`, `ecs:DescribeServices`, `ecs:DescribeTasks` and scoped `iam:PassRole`; attach with `scripts/attach-ecs-reset-run-task-policy-to-user.ps1` when needed
- the reset path does not run unless the application boots successfully against the RDS datasource
- the reset path preserves the schema and only clears application data through the existing bootstrap reset flow

Expected result:
- `internal-core` organization exists
- `vanderson.verza@gmail.com` exists as the internal admin bootstrap user
- no seeded sample users remain when `APP_BOOTSTRAP_SEED_DATA=false`
- project, document, external organization, external user, membership and audit runtime data are cleared
- baseline project frameworks remain: `APQP`, `VDA_MLA`, `CUSTOM`
- baseline project templates remain: `TMP-APQP-V1`, `TMP-VDA-MLA-V1`, `TMP-CUSTOM-V1`
- baseline project structure templates and levels remain: `PST-APQP-V1`, `PST-VDA-MLA-V1`, `PST-CUSTOM-V1`
- non-baseline project frameworks, project templates and project structure templates are cleared
- tracked document S3 objects and orphan objects under the configured document key prefix are deleted

Last verified:
- `2026-04-28`: `dev-reset.cmd`/ECS one-off flow completed successfully against task definition `program-management-system:71`, and `https://api.oryzem.com/public/ping` returned `public-ok` afterward.
