# Application / Infrastructure Coupling Assessment

## Goal

Track where `application` still depends directly on persistence entities, Spring Data repositories, or other infrastructure classes so we can keep pushing the module toward ports/adapters.

## Checkpoint 2026-04-13

End-of-day status:

- The `application` layer no longer injects `SpringDataProjectDeliverableJpaRepository`, `SpringDataProjectMemberJpaRepository`, or `SpringDataProjectOrganizationJpaRepository` directly.
- The deliverable slice was migrated to `ProjectDeliverableRepository`.
- The member/organization slice was migrated to `ProjectMemberRepository` and `ProjectOrganizationRepository`.
- The main remaining direct Spring Data dependency inside `application` is `ProjectIdempotencyService`.
- The main architectural debt that remains is entity leakage through ports: several `application.port` contracts still expose JPA entities from `infrastructure`.

Recommended restart point for the next session:

1. Refactor `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/ProjectIdempotencyService.java` behind a dedicated port.
2. Start reducing entity leakage in ports, prioritizing:
   - `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/port/ProjectRepository.java`
   - `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/port/ProjectMemberRepository.java`
   - `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/port/ProjectOrganizationRepository.java`
3. After that, revisit policies/mappers that still use infrastructure entities as their working model.

## Severity Levels

- `High`: `application` injects `SpringData...JpaRepository` directly.
- `Medium`: `application` depends on `*Entity` types from infrastructure in service/policy signatures or control flow.
- `Foundational`: `application.port` exists, but still exposes infrastructure entities as its contract.

## Current Hotspots

### High

At the moment, the project management `application` package no longer injects `SpringDataProjectDeliverableJpaRepository`, `SpringDataProjectMemberJpaRepository`, or `SpringDataProjectOrganizationJpaRepository` directly.

The remaining `High`-severity work is now concentrated in other concrete infrastructure dependencies outside this specific slice and in ports that still leak persistence entities.

### Medium

- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/ProjectAccessPolicy.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/ProjectDeliverableAccessPolicy.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/ProjectMilestoneAccessPolicy.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/ProjectStructureNodeAccessPolicy.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/ProjectVisibilityPolicy.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/ProjectViewMapper.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/ProjectTemplateInstantiationService.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/GetProjectStructureTreeUseCase.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/GetProjectDashboardUseCase.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/query/GetProjectDashboardQuery.java`

These classes no longer inject Spring Data directly in every case, but they still operate on persistence entities as their working model.

### Foundational

The ports below already help reduce direct Spring Data usage, but they still expose infrastructure entities and therefore are not yet true anti-corruption boundaries:

- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/port/ProjectRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/port/ProjectDeliverableRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/port/ProjectMemberRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/port/ProjectMilestoneRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/port/ProjectOrganizationRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/port/ProjectStructureNodeRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/port/DeliverableSubmissionRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/port/DeliverableSubmissionDocumentRepository.java`

## First Refactoring Slice Applied

We started with the lowest-risk deliverable slice because it already had a working adapter:

- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/UpdateDeliverableUseCase.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/ListPendingReviewDeliverablesUseCase.java`

Both now depend on `ProjectDeliverableRepository` instead of `SpringDataProjectDeliverableJpaRepository`.

## Second Refactoring Slice Applied

We then moved the member/organization collaboration flow onto ports:

- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/AddProjectMemberUseCase.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/AddProjectOrganizationUseCase.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/CreateProjectUseCase.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/ListProjectsUseCase.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/ProjectAuthorizationService.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/query/ListProjectSummariesQuery.java`

Supported by the new ports/adapters:

- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/port/ProjectMemberRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/port/ProjectOrganizationRepository.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/infrastructure/JpaProjectMemberRepositoryAdapter.java`
- `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/infrastructure/JpaProjectOrganizationRepositoryAdapter.java`

## Recommended Next Slices

1. Reduce entity leakage in the existing ports by returning aggregates/read models instead of JPA entities.
2. Revisit policy/mapper classes that still use infrastructure entities as their primary working model.
3. Decide whether `ProjectViewMapper` should stay in `application` or move behind a clearer read-model boundary.
