# Project Visibility Rules

`ProjectVisibilityScope` is the single semantic source for project-scoped artifact visibility.

## Scopes

| Scope | Can view | Cannot view | Applies to | Inheritance |
| --- | --- | --- | --- | --- |
| `INTERNAL_ONLY` | only platform-internal privileged actors | every external project actor, including participants | deliverables, milestones and structure nodes | never inherited to widen access |
| `ALL_PROJECT_PARTICIPANTS` | active project organizations (`LEAD`, `CUSTOMER`, `SUPPLIER`, `PARTNER`) and explicit project members | non-participants | deliverables, milestones and structure nodes | child entity can override with a narrower scope |
| `RESPONSIBLE_AND_APPROVER` | lead organization, project managers/coordinators, and the resource-specific assignment (`responsible` / `approver` / `owner`, when present) | unrelated participant organizations and viewers outside the assignment | deliverables, milestones and structure nodes | child entity can override project default |
| `LEAD_ONLY` | lead organization, project managers/coordinators | customer, supplier and partner participants that are not managers | deliverables, milestones and structure nodes | child entity can override project default |

## Notes

- `ProjectVisibilityPolicy` in `src/main/java/com/oryzem/programmanagementsystem/modules/projectmanagement/application/ProjectVisibilityPolicy.java` is the runtime authority for these rules.
- Project-level permission checks still decide whether an actor belongs to the project at all; visibility only narrows access to scoped artifacts.
- Submission and document access reuse the same deliverable visibility baseline before applying action-specific authorization.
- For milestones, `ownerOrganizationId` is the resource-specific assignment used by `RESPONSIBLE_AND_APPROVER`.
- For structure nodes, `ownerOrganizationId` and `responsibleUserId` are the resource-specific assignments used by `RESPONSIBLE_AND_APPROVER`.
- Structural tree reads must hide the entire subtree when a parent node is not visible; children are not promoted to the top level.
- When a read is scoped by `structureNodeId` and the actor cannot view that node, the API must fail with `403 Forbidden` instead of returning an empty filtered payload.
- Detail endpoints must enforce the scoped entity visibility directly (`deliverable`, `milestone`, `structure node`, `submission`) and must not rely only on broad project access.
