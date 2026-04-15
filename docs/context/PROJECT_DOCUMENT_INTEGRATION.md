# Project Document Integration

## Supported Contexts
- `PROJECT`: project-level document
- `PROJECT_DELIVERABLE`: working document, operational evidence, work in progress
- `PROJECT_DELIVERABLE_SUBMISSION`: formal submission package and review evidence

## Integration Contract
- Project-management must not read document tables directly.
- Validation happens through `DocumentPublicFacade` consumed by `ProjectDocumentValidationService`.
- The published document read contract includes `DocumentView`, `DocumentStatus`, and `DocumentContextType`.
- Project-owned document contexts are registered through the public SPI `DocumentContextPolicyProvider`, which returns `DocumentContextPolicy`.

## Submission Validation Rules
For every `documentId` submitted:
- document exists
- status is `ACTIVE`
- tenant matches the project tenant
- document is accessible to the current actor
- context must be exactly `PROJECT_DELIVERABLE_SUBMISSION`
- `contextId` must be the target submission id
- `PROJECT_DELIVERABLE` documents are not accepted as formal submission documents

## Ownership Rule
- Document-management owns storage, status, and document visibility checks.
- Project-management owns when a document is acceptable for a project workflow.

## Workflow Rule
- `PROJECT_DELIVERABLE` remains the context for operational/work documents.
- A formal submission may be created first and receive its formal documents in the `PROJECT_DELIVERABLE_SUBMISSION` context afterward.
- Review/audit semantics must treat the submission package as distinct from working evidence.

## Future Rule
- Any new module integrating with documents should use the published document contract/SPI, never document JPA entities or internal adapters.
