# Project Management Published Interface

## Public HTTP Surface
- `ProjectController`: create, detail, list, update project.
- `ProjectParticipantController`: add/list organizations and members.
- `ProjectStructureController`: read tree, create/update/move nodes.
- `ProjectMilestoneController`: list and update milestones.
- `ProjectDeliverableController`: list/detail/update deliverables, pending-review queue, responsible queue.
- `DeliverableSubmissionController`: submit, list, detail, approve, reject.
- `ProjectDashboardController`: project dashboard read model.
- `ProjectTemplateController`: project template and artifact template administration.
- `ProjectStructureTemplateController`: structure template administration.

## Allowed Code-Level Integration
- External modules must not depend on `projectmanagement.infrastructure`.
- External modules must not consume project JPA entities.
- At the moment, project-management exposes its public surface primarily via HTTP endpoints.
- Internal persistence adapters are replaceable details, not contracts.

## Public Providers Consumed By Project
- Document integration is allowed only through `documentmanagement.application.DocumentPublicFacade`.
- The document published contract also includes the immutable DTOs/enums exposed by that facade:
  `documentmanagement.application.DocumentView`,
  `documentmanagement.domain.DocumentStatus`,
  `documentmanagement.domain.DocumentContextType`.
- The document extension SPI used to register project-owned contexts is also public by design:
  `documentmanagement.domain.DocumentContextPolicyProvider`,
  `documentmanagement.domain.DocumentContextPolicy`.

## Internal Packages
- Forbidden for external consumption: `projectmanagement.infrastructure`
- Forbidden for external consumption: `projectmanagement.domain`
- Forbidden for external consumption: `projectmanagement.application` except when a future explicit published contract is introduced
- Forbidden for external consumption: `projectmanagement.config`
- Forbidden for external consumption: `projectmanagement.support`

## Evolution Rule
- New cross-module integrations must first define a published contract and corresponding architecture test.
- If another module needs project-management data synchronously, prefer an explicit facade/query contract over direct package access.
