# Modular Monolith Refactor

## Objective
Keep one Spring Boot application and one deployment unit while enforcing a smaller, explicit runtime boundary around the active core.

## Current structure

### `com.oryzem.programmanagementsystem.app`
Global bootstrap and cross-cutting application wiring.

### `com.oryzem.programmanagementsystem.platform.auth`
Authentication, Cognito integration, security filters and auth API.

### `com.oryzem.programmanagementsystem.platform.authorization`
Authorization model, matrix, roles and decision service.

### `com.oryzem.programmanagementsystem.platform.audit`
Audit persistence and request correlation.

### `com.oryzem.programmanagementsystem.platform.users`
User lifecycle, synchronization and identity-provider integration.

### `com.oryzem.programmanagementsystem.platform.access`
Memberships, active context, tenant summaries and market administration.

### `com.oryzem.programmanagementsystem.platform.tenant`
Organization hierarchy, lookup, bootstrap and purge support.

### `com.oryzem.programmanagementsystem.platform.shared`
Small neutral cross-module contracts.

## Removed modules
The following runtime areas were removed because they no longer belong to the active product boundary:
- `modules.projectmanagement`
- `modules.operations`
- `modules.reports`
- `platform.documents`

## Active dependency rules
- `app` wires modules and cross-cutting services but should not own business rules
- `platform.auth` may depend on authorization, audit, access and users
- `platform.users` depends on authorization, audit and access/tenant lookup contracts
- `platform.access` depends on authorization, tenant lookup and users domain contracts
- `platform.tenant` depends on authorization, audit, access provisioning and user-side tenant ports
- `platform.shared` stays neutral

## Ports worth keeping explicit
- `platform.tenant.OrganizationLookup`
- `platform.tenant.OrganizationBootstrapPort`
- `platform.tenant.OrganizationResetPort`
- `platform.tenant.TenantUserQueryPort`
- `platform.tenant.TenantUserPurgePort`
- `platform.users.domain.UserIdentityGateway`

## Validation
Executed after the cleanup:
- `./mvnw.cmd test`

Result:
- build passed
- core test suite passed
