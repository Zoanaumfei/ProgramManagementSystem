# Project Structure Frontend Handoff

## Product decision
- Keep the existing backend model and officially expose the runtime project structure flow to the frontend.
- `StructureLevel` remains template metadata that defines the hierarchy, for example `Project` then `Parts`.
- `ProjectStructureNode` is the concrete project item created at runtime, for example one specific part/product under the root project node.
- Do not change project creation for now; root-level artifacts are created during project creation, and structure-level artifacts are created when the matching runtime node is created.

## Runtime behavior
- Every project starts with one root node based on the first structure level of the linked structure template.
- A template deliverable with `appliesToType = ROOT_NODE` is materialized on the root node during project creation.
- A template deliverable with `appliesToType = STRUCTURE_LEVEL` is materialized only when a runtime node with the matching `structureLevelTemplateId` is created.
- For the common `Project -> Parts` hierarchy, the frontend creates a child under the root `Project` node; the backend derives that the child belongs to the `Parts` level.
- The frontend must not send or ask the user for `levelTemplateId` when creating a runtime node.

## Endpoints
- `GET /api/projects/{projectId}/structure`
- `POST /api/projects/{projectId}/structure/nodes`
- `PATCH /api/projects/{projectId}/structure/nodes/{nodeId}`
- `POST /api/projects/{projectId}/structure/nodes/{nodeId}/move`

## Create node payload
```json
{
  "parentNodeId": "PRJ-123-ROOT",
  "name": "Brake Pedal Assembly",
  "code": "PART-001",
  "ownerOrganizationId": "ORG-123",
  "responsibleUserId": "USR-123",
  "visibilityScope": "ALL_PROJECT_PARTICIPANTS"
}
```

## Frontend implementation notes
- Add a runtime structure panel/tab in project detail.
- Render `levels` and `nodes` from `GET /api/projects/{projectId}/structure` as a tree.
- Label create actions from the next level when possible, for example `Adicionar Part`.
- After creating a node, refresh structure, dashboard, milestones, deliverables and review queues.
- When a node is selected, pass `structureNodeId` to scoped project reads such as dashboard, milestones, deliverables, pending-review and responsible queues.
- Empty state for a level with no runtime nodes should explain that creating a node materializes the deliverables configured for that level.

## Authorization notes
- Runtime node create/update/move requires project edit permission.
- In the current backend this means project role `PROJECT_MANAGER` or `COORDINATOR`.
- `EXTERNAL ADMIN` by itself is not a project-management bypass; only internal privileged actors bypass normal project authorization.

## Current documentation status
- The API contract has been updated in `docs/context/API_CONTRACT.md`.
- Frontend alignment expectations are tracked in `docs/context/FRONTEND_BACKEND_ALIGNMENT.md`.
- The remaining frontend implementation work is tracked in `docs/context/OPEN_GAPS.md`.
