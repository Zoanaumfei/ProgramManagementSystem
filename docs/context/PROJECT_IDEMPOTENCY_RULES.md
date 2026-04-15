# Project Idempotency Rules

Operations that currently require or support `Idempotency-Key`:

- `POST /api/projects`
- `POST /api/projects/{projectId}/deliverables/{deliverableId}/submissions`
- `POST /api/projects/{projectId}/deliverables/{deliverableId}/submissions/{submissionId}/approve`
- `POST /api/projects/{projectId}/deliverables/{deliverableId}/submissions/{submissionId}/reject`

Rules:

- idempotency is scoped by `tenantId + operation + idempotencyKey`
- the request body hash must match the original request
- the persisted payload is wrapped in a controlled structure with response type metadata
- persisted payload size is capped in `ProjectIdempotencyService.MAX_PERSISTED_PAYLOAD_CHARS`
