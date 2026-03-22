# Modular Monolith Refactor

## Objective
The codebase now follows a modular-monolith direction while preserving:
- one Spring Boot application
- one deployment unit
- existing HTTP contracts
- existing Flyway migrations and schema
- current business behavior

This document reflects the state after the latest consolidation pass on 2026-03-22.

Baseline note:
- the current modular-monolith structure is now accepted as the working baseline
- future work should prioritize product evolution, not another broad architectural reshuffle

## Final Structure

### `com.oryzem.programmanagementsystem.app`
Global bootstrap and truly cross-cutting application wiring.

Current files:
- `src/main/java/com/oryzem/programmanagementsystem/app/ProgramManagementSystemApplication.java`
- `src/main/java/com/oryzem/programmanagementsystem/app/api/PingController.java`
- `src/main/java/com/oryzem/programmanagementsystem/app/web/ApiExceptionHandler.java`
- `src/main/java/com/oryzem/programmanagementsystem/app/bootstrap/BootstrapDataService.java`
- `src/main/java/com/oryzem/programmanagementsystem/app/config/RdsSecretEnvironmentPostProcessor.java`

### `com.oryzem.programmanagementsystem.platform.shared`
Small neutral cross-module contracts.

Current files:
- `src/main/java/com/oryzem/programmanagementsystem/platform/shared/ResourceNotFoundException.java`

### `com.oryzem.programmanagementsystem.platform.auth`
Authentication, Cognito integration, security filters and auth API.

Current files:
- `src/main/java/com/oryzem/programmanagementsystem/platform/auth/SecurityConfig.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/auth/CognitoProperties.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/auth/CognitoJwtAuthenticationConverter.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/auth/CognitoAudienceValidator.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/auth/AuthenticatedUserSynchronizationFilter.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/auth/api/AuthController.java`

### `com.oryzem.programmanagementsystem.platform.authorization`
Authorization model, matrix, roles and decision service.

Current files:
- `src/main/java/com/oryzem/programmanagementsystem/platform/authorization/AuthorizationService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/authorization/AuthorizationMatrix.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/authorization/api/AuthorizationController.java`

### `com.oryzem.programmanagementsystem.platform.audit`
Audit persistence and request correlation.

Current files:
- `src/main/java/com/oryzem/programmanagementsystem/platform/audit/AuditTrailService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/audit/RequestCorrelationFilter.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/audit/AuditLogEntity.java`

### `com.oryzem.programmanagementsystem.platform.users`
User management capability.

Current files:
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/api/CreateUserRequest.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/api/UpdateUserRequest.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/api/UserActionResponse.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/api/UserSummaryResponse.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/api/UserManagementController.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/application/UserManagementService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/application/UserQueryService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/application/UserCommandService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/application/UserSensitiveActionService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/application/UserPurgeService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/application/UserAccessService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/application/AuthenticatedUserSynchronizationService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/domain/UserRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/domain/UserIdentityGateway.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/domain/ManagedUser.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/domain/UserStatus.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/domain/UserNotFoundException.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/infrastructure/JpaUserRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/infrastructure/SpringDataUserJpaRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/infrastructure/UserEntity.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/infrastructure/UserIdentityConfig.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/infrastructure/StubUserIdentityGateway.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/infrastructure/CognitoUserIdentityGateway.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/infrastructure/TenantUserPortAdapter.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/users/infrastructure/ReportUserQueryAdapter.java`

Current design note:
- `platform.users` is now the clearest example of the intended modular-monolith layering
- HTTP contract stayed intact while DTOs, application orchestration, domain contracts and infrastructure adapters moved to coherent subpackages

### `com.oryzem.programmanagementsystem.platform.tenant`
Organization hierarchy, subtree visibility and tenant directory.

Current files:
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/PortfolioOrganizationService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/OrganizationAccessService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/OrganizationQueryService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/OrganizationCommandService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/OrganizationPurgeService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/OrganizationSnapshotService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/OrganizationDirectoryService.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/OrganizationBootstrapPort.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/OrganizationLookup.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/OrganizationRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/TenantProjectPortfolioPort.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/TenantUserQueryPort.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/TenantUserPurgePort.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/tenant/TenantOrganizationController.java`

### `com.oryzem.programmanagementsystem.platform.documents`
Reusable document-storage capability.

Current files:
- `src/main/java/com/oryzem/programmanagementsystem/platform/documents/PortfolioDocumentStorageGateway.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/documents/PortfolioDocumentStorageConfig.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/documents/DocumentStorageObject.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/documents/PreparedDocumentUpload.java`
- `src/main/java/com/oryzem/programmanagementsystem/platform/documents/PreparedDocumentDownload.java`

### `com.oryzem.programmanagementsystem.modules.projectmanagement`
Portfolio and project-management module.

Current files:
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/PortfolioManagementController.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/PortfolioManagementService.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/PortfolioResetPort.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/PortfolioRepositories.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/ProjectManagementTenantPortfolioAdapter.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/ProgramEntities.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/ExecutionEntities.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/GovernanceEntities.java`

Internal subdomains created in this second pass:
- `program`: `PortfolioProgramService`
- `project`: `PortfolioProjectService`
- `execution`: `PortfolioExecutionService`
- `governance`: `PortfolioGovernanceService`
- shared application support: `ProjectManagementAccessService`, `ProjectManagementLookupService`, `ProjectManagementValidationSupport`
- templates/configuration: `PortfolioMilestoneTemplateService`

Current design note:
- the internal split is expressed primarily by focused services inside the module root package
- grouped JPA entities remain package-private in the module root to avoid widening visibility just to satisfy package layering mechanically
- this keeps the refactor safe while still reducing concentration and coupling

### `com.oryzem.programmanagementsystem.modules.operations`
Operations module.

Current files:
- `src/main/java/com/oryzem/programmanagementsystem/modules/operations/OperationManagementService.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/operations/OperationRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/operations/api/OperationManagementController.java`

### `com.oryzem.programmanagementsystem.modules.reports`
Reports module.

Current files:
- `src/main/java/com/oryzem/programmanagementsystem/modules/reports/ReportManagementService.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/reports/ReportUserQueryPort.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/reports/ReportOperationQueryPort.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/reports/api/ReportController.java`

## Allowed Dependencies
- `app` may wire all modules, but should not own business rules
- `platform.auth` may depend on `platform.authorization`, `platform.audit`, and `platform.users`
- `platform.users` depends on `platform.authorization`, `platform.audit`, and tenant-owned lookup/query contracts
- `platform.tenant` may depend on `platform.authorization`, `platform.audit`, and `TenantProjectPortfolioPort`
- `platform.documents` must stay reusable and should not depend on projectmanagement JPA entities
- `modules.projectmanagement` may depend on platform capabilities only
- `modules.operations` may depend on platform capabilities only
- `modules.reports` may depend on platform capabilities and read-side ports only

## Interfaces Between Modules

### `platform.tenant.OrganizationLookup`
Purpose:
- make tenant lookup ownership explicit in the tenant module

Implemented by:
- `platform.tenant.OrganizationDirectoryService`

Used by:
- `platform.users.UserManagementService`
- `modules.projectmanagement.ProjectManagementAccessService`
- `modules.reports.ReportManagementService`

### `platform.tenant.TenantUserQueryPort`
Purpose:
- let `platform.tenant` ask user-side questions without using `UserRepository` or `ManagedUser`

Implemented by:
- `platform.users.TenantUserPortAdapter`

Used by:
- `platform.tenant.OrganizationDirectoryService`
- `platform.tenant.OrganizationCommandService`
- `platform.tenant.OrganizationSnapshotService`

### `platform.tenant.TenantUserPurgePort`
Purpose:
- let `platform.tenant` purge subtree users without depending on users infrastructure

Implemented by:
- `platform.users.TenantUserPortAdapter`

Used by:
- `platform.tenant.OrganizationPurgeService`

### `platform.users.UserIdentityGateway`
Purpose:
- isolate identity-provider operations from user-management orchestration

Used by:
- `platform.users.UserManagementService`

### `platform.tenant.TenantProjectPortfolioPort`
Purpose:
- allow tenant purge/reference checks without direct access to projectmanagement repositories/entities

Implemented by:
- `modules.projectmanagement.ProjectManagementTenantPortfolioAdapter`

Used by:
- `platform.tenant.PortfolioOrganizationService`

### `platform.tenant.OrganizationBootstrapPort`
Purpose:
- let bootstrap seed organizations without depending on the concrete tenant directory implementation

Implemented by:
- `platform.tenant.OrganizationDirectoryService`

Used by:
- `app.bootstrap.BootstrapDataService`

### `modules.projectmanagement.PortfolioResetPort`
Purpose:
- let bootstrap clear project-management data without binding to the concrete reset service

Implemented by:
- `modules.projectmanagement.PortfolioResetService`

Used by:
- `app.bootstrap.BootstrapDataService`

### `platform.documents.PortfolioDocumentStorageGateway`
Purpose:
- expose document storage without leaking projectmanagement JPA entities

Used by:
- `modules.projectmanagement.PortfolioExecutionService`
- `modules.projectmanagement.ProjectManagementTenantPortfolioAdapter`

### `modules.reports.ReportUserQueryPort`
Purpose:
- allow reports to read user aggregates without depending on `platform.users.UserRepository`

Implemented by:
- `platform.users.ReportUserQueryAdapter`

### `modules.reports.ReportOperationQueryPort`
Purpose:
- allow reports to read operations without depending on `modules.operations.OperationRepository`

Implemented by:
- `modules.operations.ReportOperationQueryAdapter`

### `modules.projectmanagement.PortfolioResetTenantPort`
Purpose:
- let projectmanagement clear tenant data in reset flows without importing `OrganizationRepository`

Implemented by:
- `platform.tenant.TenantPortfolioResetAdapter`

## Key Decisions Taken In The Latest Pass
- accepted the current modular-monolith layout as the baseline and stopped short of another broad package redesign
- reduced bootstrap coupling by switching `BootstrapDataService` from concrete tenant/projectmanagement services to small module-facing ports
- added ArchUnit guardrails for app/config scope, bootstrap boundaries, generic web buckets, shared neutrality and concrete repository leakage
- kept the detailed shared context in `docs/context/` and preserved the root `PROJECT_CONTEXT.md` as a short entrypoint
- consolidated `platform.users` into explicit `api/application/domain/infrastructure` packages without changing behavior or contracts
- moved user DTOs into `api`, orchestration services into `application`, core contracts into `domain`, and JPA/identity adapters into `infrastructure`
- split stub and Cognito identity implementations into dedicated infrastructure files so tests no longer depend on package-private helpers
- physically moved source and test files so directories now match current packages
- kept `modules.projectmanagement` as one module, but split orchestration into focused services by subdomain
- moved `OrganizationLookup` ownership from `platform.users` to `platform.tenant`
- replaced remaining tenant-to-users direct dependencies with `TenantUserQueryPort` and `TenantUserPurgePort`
- replaced reports direct repository/domain access with `ReportUserQueryPort` and `ReportOperationQueryPort`
- replaced projectmanagement direct tenant repository access in reset flows with `PortfolioResetTenantPort`
- moved business-level not-found handling out of `app` into `platform.shared.ResourceNotFoundException`
- broke `PortfolioOrganizationService` into query, command, purge, access and snapshot services
- broke `UserManagementService` into query, command, sensitive-action, purge, synchronization and access services
- removed stale projectmanagement-only organization request DTOs and duplicate enums that no longer belong to the module
- kept grouped JPA entity files package-private instead of exploding them into dozens of public classes purely for folder symmetry

## Remaining Couplings And Real Debt
- `platform.users` is layered internally now, but `platform.tenant` and `platform.documents` still need the same level of physical/internal separation
- `modules.projectmanagement` still keeps grouped JPA entities and grouped response records in the module root package
- request/response/entity packages are not yet fully split into `api/application/domain/infrastructure`; the current split prioritizes safe decomposition over mechanical package proliferation
- document response DTOs still belong to projectmanagement because the public contract depends on projectmanagement metadata and enums
- `platform.tenant`, `platform.users` and `platform.documents` still mix domain and infrastructure classes in the module root even though the orchestration layer is now less concentrated
- `app.bootstrap.BootstrapDataService` still reads `UserRepository` and `OperationRepository` directly; this is accepted as bootstrap wiring because both are module contracts, not infrastructure implementations
- deeper package layering for `tenant`, `documents` and `projectmanagement` is intentionally deferred; the current state is considered good enough for returning focus to product work

## Next Refactor Candidates
- only pursue further refactor when a product need or concrete maintenance pain justifies it
- if architecture work returns later, prefer narrow changes tied to a specific feature/module instead of another repo-wide reshape

## Validation
Executed after the latest pass:
- `./mvnw.cmd -q -DskipTests compile`
- `./mvnw.cmd -q test`

Result:
- compilation passed
- existing test suite passed
