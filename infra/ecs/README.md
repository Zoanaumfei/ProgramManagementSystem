# ECS/Fargate Core Deploy

Current deployment target is the core SaaS administration runtime only.

## Container configuration kept in the task definition
- `SPRING_PROFILES_ACTIVE=rds`
- `DB_SECRET_ID=program-management-system/rds/master`
- `AWS_REGION=sa-east-1`
- `DB_SSL_MODE=require`
- `APP_SECURITY_IDENTITY_PROVIDER=cognito`
- `APP_SECURITY_IDENTITY_USER_POOL_ID=sa-east-1_aA4I3tEmF`
- `APP_SECURITY_IDENTITY_REGION=sa-east-1`

## What is intentionally gone
- portfolio document storage environment variables
- document bucket provisioning as part of the core deploy path
- deployment assumptions based on legacy tenant claims in the JWT

## Authentication notes
- the backend depends on Cognito for identity and groups
- tenant, organization and membership context are resolved from local membership data
- the pre-token Lambda still publishes `username`, `email` and `user_status` to support invited-user reconciliation

## Operational checks after deploy
1. validate `/public/ping`
2. validate `/api/auth/me` with a real access token
3. validate `/api/access/users` and `/api/access/organizations` with an internal admin token
4. confirm audit logging, quota handling and tenant rate limiting in the target environment

## Important IAM note
The task role still needs Cognito administrative permissions used by the active core user lifecycle, including:
- `AdminCreateUser`
- `AdminUpdateUserAttributes`
- `AdminAddUserToGroup`
- `AdminRemoveUserFromGroup`
- `AdminResetUserPassword`
- `AdminSetUserPassword`
- `AdminDisableUser`
- `AdminGetUser`
- `AdminDeleteUser`

## Suggested deploy sequence
1. ensure the ECR repository exists
2. build and push the image with `scripts/deploy-to-ecs-fargate.ps1`
3. render the task definition with `scripts/render-ecs-task-definition.ps1`
4. update or create the ECS service
5. validate the authenticated core flows and runbook references
