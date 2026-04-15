# Project Authorization Matrix

## Roles
- `PROJECT_MANAGER`: broad management over project execution.
- `COORDINATOR`: operational management inside assigned organization.
- `RESPONSIBLE`: works on assigned deliverables.
- `APPROVER`: reviews and decides on submissions.
- `VIEWER`: read-only participant.

## Resource Actions
- Project: `VIEW_PROJECT`, `EDIT_PROJECT`
- Milestone: `VIEW_MILESTONE`, `EDIT_MILESTONE`
- Deliverable: `VIEW_DELIVERABLE`, `EDIT_DELIVERABLE`, `SUBMIT_DELIVERABLE`, `UPLOAD_DOCUMENT`
- Submission: `VIEW_SUBMISSION`, `REVIEW_SUBMISSION`, `APPROVE_SUBMISSION`, `REJECT_SUBMISSION`
- Document visibility follows project visibility and assignment rules.

## Functional Summary
- Managers and coordinators can manage project, structure, milestones, and deliverables within project scope.
- Responsible actors can work on assigned deliverables and submit when visibility/assignment rules allow.
- Approvers can review and decide submissions when allowed by assignment and visibility.
- Viewers can read only what visibility allows.
- Internal privileged users bypass normal project role checks.

## Source Of Truth
- Authorization facade: `ProjectAuthorizationService`
- Resource policies: `ProjectAccessPolicy`, `ProjectMilestoneAccessPolicy`, `ProjectDeliverableAccessPolicy`, `DeliverableSubmissionAccessPolicy`, `ProjectStructureNodeAccessPolicy`
- Visibility semantics: `ProjectVisibilityPolicy`

## Test Coverage
- `ProjectAuthorizationMatrixTest`
- `DeliverableAuthorizationMatrixTest`
- `SubmissionAuthorizationMatrixTest`
