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
- `APP_BOOTSTRAP_INTERNAL_ADMIN_ENABLED=false`
- `APP_BOOTSTRAP_INTERNAL_ADMIN_PRUNE_OTHER_INTERNAL_USERS=false`
- `APP_BOOTSTRAP_INTERNAL_ADMIN_PRUNE_TO_INTERNAL_CORE=false`
- `APP_DOCUMENT_MANAGEMENT_S3_BUCKET=oryzem-pms-documents-sa-east-1-439533253319`
- `APP_DOCUMENT_MANAGEMENT_S3_REGION=sa-east-1`
- `APP_PROJECT_MANAGEMENT_DOCUMENTS_ENABLED=true`

## What is intentionally gone
- portfolio document storage environment variables
- deployment assumptions based on legacy tenant claims in the JWT

## Document storage
Run `scripts/configure-document-storage-s3.ps1` to provision or reconcile the document bucket and the ECS task role access.

Current document bucket:
- `oryzem-pms-documents-sa-east-1-439533253319`

The bucket configuration enforced by the script is:
- private bucket with all public access blocked
- `BucketOwnerEnforced` object ownership
- versioning enabled
- default SSE-KMS encryption with bucket keys
- CORS for `http://localhost:3000`, `https://oryzem.com` and `https://www.oryzem.com`

## Authentication notes
- the backend depends on Cognito for identity and groups
- tenant, organization and membership context are resolved from local membership data
- the pre-token Lambda still publishes `username`, `email` and `user_status` to support invited-user reconciliation

## Operational checks after deploy
1. validate `/public/ping`
2. validate `/api/auth/me` with a real access token
3. validate `/api/access/users` and `/api/access/organizations` with an internal admin token
4. confirm audit logging, quota handling and tenant rate limiting in the target environment
5. validate `POST /api/document-contexts/{contextType}/{contextId}/documents/uploads` with a real access token

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

## Public HTTPS on the ALB
The ECS service does not provision the load balancer listener itself.
Use `scripts/enable-alb-https.ps1` to attach an ACM certificate, add the HTTPS listener on port `443`, and redirect `80 -> 443`.

Example:
```powershell
.\scripts\enable-alb-https.ps1 `
  -LoadBalancerArn arn:aws:elasticloadbalancing:sa-east-1:439533253319:loadbalancer/app/program-management-system-alb/... `
  -CertificateArn arn:aws:acm:sa-east-1:439533253319:certificate/... `
  -TargetGroupArn arn:aws:elasticloadbalancing:sa-east-1:439533253319:targetgroup/program-management-system-alb-tg/1425d73086a3393d
```

The published frontend should then use `https://` for the API base URL.

## Historical reference

The full published API HTTPS setup, DNS, ALB, certificate and verification history are documented in `docs/runbooks/PUBLIC_API_HTTPS.md`.
