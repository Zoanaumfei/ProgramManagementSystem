# Frontend Backend Alignment

This repository now exposes only the core membership-first platform surface.

## Backend contract verified in this repository
- current user and active context: `GET /api/auth/me`
- request-scoped context switch: `POST /api/access/context/activate`
- user account admin: `/api/access/users`
- orphan cleanup discovery: `GET /api/access/users/orphans`
- membership admin: `/api/access/users/{userId}/memberships`
- organization admin: `/api/access/organizations`
- organization relationships: `/api/access/organizations/{organizationId}/relationships`
- organization export operations: `/api/access/organizations/{organizationId}/exports`
- tenant directory: `GET /api/access/tenants`
- tenant service tier updates: `PATCH /api/access/tenants/{tenantId}/service-tier`
- tenant market admin: `/api/access/tenants/{tenantId}/markets`
- project management: `/api/projects`
- project structure nodes: `/api/projects/{projectId}/structure`
- project framework catalog: `/api/project-frameworks`
- project purge administration: `/api/projects/{projectId}/purge-intents` and `/api/projects/{projectId}/purge`
- document management for project contexts: `/api/document-contexts/{contextType}/{contextId}/documents` and `/api/documents/{documentId}*`

## Backend behaviors the frontend must honor
- frontend should treat membership APIs as the main access-management surface
- frontend should treat organization administration as part of the core access shell
- frontend should rely exclusively on the active access-first surface documented in the backend contract
- frontend should send `X-Access-Context` when operating under a request-scoped selected membership
- invalid `X-Access-Context` values fail closed with `400 Bad Request`
- frontend should rely on structured `401`/`403` payloads with `message`, `path` and `correlationId` to explain authorization failures to admins
- the operational dashboard backend contract is implemented at `GET /api/admin/operational/overview`
- the operational overview accepts `from`, `to`, repeated `tenantId`, `tenantTier`, `path` and `activeOnly` filters
- the operational dashboard receives `kpis`, `series`, `topTenants`, `tenantDetails`, `alerts` and `recentEvents` from the aggregated backend response
- frontend should require `organizationId` and `roles` when creating users because the backend now provisions the first membership in the same request
- frontend project creation should treat the active access-context organization as the lead organization, because the backend resolves lead ownership from the authenticated context instead of a request field
- frontend should load `/api/project-frameworks` as the authoritative framework catalog for project UX/UI presentation metadata and use the returned `code` values to interpret `frameworkType` fields from project and template payloads
- frontend should treat `frameworkType` as a stable framework code string, not as a closed enum baked into the client
- frontend should use `displayName` for operator-facing labels and `uiLayout` to choose the project workspace presentation mode
- framework catalog management is now available only for internal `ADMIN` operators through `GET|POST|PATCH /api/project-frameworks`
- inactive frameworks may still appear in historical data, but the frontend should not present them as options for new project or template creation
- frontend should treat `/api/project-templates` and `/api/project-structure-templates` as the administrative source of truth for template governance and for the project-create wizard, with the visible/selectable catalog scoped by the active organization context and backend ownership rules
- frontend should assume template catalog endpoints already return only use-authorized templates and should not surface non-returned templates as selectable fallbacks
- frontend should treat template-management actions (edit/purge/activate/deactivate and template-child mutations) as owner-only operations even for tenant admins outside the owner organization
- frontend should treat templates as setup accelerators, not as locks: after project creation, project edits and runtime milestone/deliverable create/update/delete actions are project-local and must use `/api/projects/{projectId}`, `/api/projects/{projectId}/milestones*` and `/api/projects/{projectId}/deliverables*` rather than template-management endpoints
- frontend now offers add/remove milestone and add/remove deliverable actions in the project runtime workspace; deletion calls the runtime `DELETE` endpoints and surfaces backend business blockers, especially `PROJECT_MILESTONE_IN_USE`, `PROJECT_DELIVERABLE_HAS_SUBMISSIONS` and `PROJECT_DELIVERABLE_HAS_DOCUMENTS`
- frontend should prefer setting deliverables to `WAIVED` instead of deletion once a deliverable has submissions/documents or otherwise needs audit history preserved
- frontend should expose project phases in the runtime project workspace at least as a read-only grouping by the `phaseId` fields returned on runtime milestones and deliverables; project-template phase endpoints remain model administration and should not be used for project-local customization
- frontend should display `templateId` and `templateVersion` as immutable provenance on a project; it should not offer template re-selection from the project edit flow
- frontend should use backend `version` fields when updating projects, deliverables and submissions, and should surface a dedicated optimistic-concurrency conflict message
- frontend now also uses backend `version` fields when updating project milestones from the project detail milestones tab
- frontend should expose the project runtime structure tree from `GET /api/projects/{projectId}/structure` and allow project managers/coordinators to create concrete child nodes such as `Parts` with `POST /api/projects/{projectId}/structure/nodes`
- frontend should not ask users to choose `levelTemplateId` when creating a project structure node; the backend derives the next level from the selected parent node
- frontend should refresh structure, dashboard, milestone, deliverable and pending-review panels after node mutations because the backend materializes structure-level milestones and deliverables as part of the create-node transaction
- frontend should pass `structureNodeId` to dashboard, milestone, deliverable and pending-review queries when the operator scopes the project detail view to a selected node
- frontend should embed document-management in the `PROJECT`, `PROJECT_DELIVERABLE` and `PROJECT_DELIVERABLE_SUBMISSION` contexts using initiate-upload -> direct storage upload -> finalize-upload -> refresh
- frontend should create deliverable submissions with an explicit `documentIds: []` when no submission-context documents exist yet; evidence files should be uploaded after creation under `PROJECT_DELIVERABLE_SUBMISSION`, not selected from `PROJECT_DELIVERABLE`
- frontend should treat project purge as a two-step destructive flow: create purge intent first, display backend impact counts, then require the operator to re-confirm with reason + confirmation text before executing the final purge request
- frontend must not call `DELETE /api/projects/{projectId}`; project removal is only available through `POST /api/projects/{projectId}/purge-intents` followed by `POST /api/projects/{projectId}/purge`
- frontend should expose project purge only to internal `ADMIN` and `SUPPORT` operators
- frontend project list views for internal `ADMIN` actors should now expect platform-wide results from `GET /api/projects`, while normal tenant actors remain tenant-scoped
- the current `ProjectSummaryResponse` still exposes `leadOrganizationId` and `customerOrganizationId` but not explicit project-tenant or organization-name fields, so the frontend currently derives list-context labels such as `tenantId` and organization names from the visible organization directory when possible
- `GET /api/access/users/orphans` is a cleanup/discovery surface for inconsistent data only, not the normal onboarding path
- frontend should treat business codes such as `ORPHAN_USER_DETECTED`, `USER_ACTIVE_MEMBERSHIP_REQUIRED` and `USER_CREATION_MEMBERSHIP_FAILED` as actionable data-repair errors rather than as recoverable inline onboarding states
- frontend users copy should frame `Usuarios sem membership` as `diagnostico e reparo`, and the memberships workspace should label `bootstrap-membership` as an exceptional repair action instead of a standard create step
- frontend administrative error messaging should include backend `correlationId` whenever those business-rule failures are shown to operators

## Organization model alignment
- frontend should require `name` and `cnpj` in organization create/update flows
- frontend should treat `cnpj` as canonical organization identity inside the active tenant
- frontend should treat organization create as a register-or-link flow by `tenant + cnpj`, with relationship creation handled by the backend when needed
- frontend should treat `localOrganizationCode` as optional metadata owned by the relationship, not the organization
- frontend should not use `parentOrganizationId`, `customerOrganizationId` or `hierarchyLevel` in forms, grids, filters, trees or breadcrumbs because those legacy hierarchy fields are no longer part of the public contract
- frontend may continue to use `childrenCount` and `hasChildren` for summaries, but only as derived relationship counters rather than as proof of a rigid tree model
- frontend copy for external organization onboarding should prefer wording such as `Cadastrar ou vincular organizacao` instead of promising that every request creates a brand-new record
- frontend should treat `PARTNER` relationships as explicit graph edges and avoid implying that they belong to the customer/supplier traversal chain

## User and membership alignment
- user create payload:

```json
{
  "displayName": "Jane Doe",
  "email": "jane@customer.com",
  "organizationId": "ORG-123",
  "marketId": null,
  "roles": ["MEMBER"]
}
```

- user update payload:

```json
{
  "displayName": "Jane Doe Updated",
  "email": "jane.updated@customer.com"
}
```

- membership create/update payloads remain tenant-scoped and include:

```json
{
  "tenantId": "TEN-tenant-a",
  "organizationId": "ORG-123",
  "marketId": null,
  "status": "ACTIVE",
  "defaultMembership": false,
  "roles": ["ADMIN"]
}
```

## Organization and relationship payload alignment
- organization create payload:

```json
{
  "name": "Gestamp",
  "cnpj": "12.345.678/0001-95",
  "status": "ACTIVE",
  "localOrganizationCode": "GESTAMP-VW-001"
}
```

- organization update payload:

```json
{
  "name": "Gestamp Brasil",
  "cnpj": "12.345.678/0001-95"
}
```

- relationship create payload:

```json
{
  "targetOrganizationId": "ORG-123",
  "relationshipType": "CUSTOMER_SUPPLIER",
  "localOrganizationCode": "DELGA-VW-001"
}
```

- relationship update payload:

```json
{
  "localOrganizationCode": "DELGA-VW-002"
}
```

## Backend response details relevant to the frontend
- `OrganizationResponse` now exposes `reused: true|false` so the UI can distinguish create versus link semantics without inferring from side effects
- the backend normalizes `cnpj` to a numeric 14-digit value in responses even when the request used a masked format
- relationship payloads and responses carry `localOrganizationCode`
- `GET /api/access/tenants` currently returns a summary model with `id`, `name`, `code`, `status`, `tenantType` and `rootOrganizationId`
- create and validation conflicts can include stable business codes such as `ORGANIZATION_CNPJ_ALREADY_EXISTS_IN_TENANT` and `ORGANIZATION_RELATIONSHIP_LOCAL_CODE_ALREADY_EXISTS`

## Frontend tracking notes
The active frontend source tree is not fully present in this repository, so the notes below are tracked alignment expectations plus the latest documented product status.

- the main workspace direction remains access-first, centered on users, organizations, markets and session diagnostics
- the users experience may expose a dedicated `Usuarios sem membership` diagnostic view backed by `/api/access/users/orphans`
- the current frontend users workspace already distinguishes normal onboarding from exceptional repair: create uses initial membership provisioning in the same request, while orphan repair routes operators into membership administration
- organization management is expected to use the directed relationship model and stop rendering a derived single-path breadcrumb for organization visibility
- frontend error helpers are expected to surface backend-provided refusal reasons and `correlationId`
- frontend administrative workspaces now surface structured `401`/`403` data from the backend without overwriting the refusal reason with a generic fallback; the visible copy preserves `message`, `path` and `correlationId` in users, organizations and session diagnostics flows
- frontend runtime config now upgrades `http` API bases to `https` when served from an `https` app origin so the published shell does not keep an insecure backend URL by accident
- the operational dashboard frontend is now active in dev and prod and consumes the minimum overview contract for `429`, `409` quota, offboarding and export requested/completed panels
- the operational dashboard frontend now also exposes the audited tenant service-tier change action for internal `ADMIN`/`SUPPORT` operators, using `PATCH /api/access/tenants/{tenantId}/service-tier`
- the frontend project-management module is now active under `/workspace/projects/*` with list, creation wizard, project detail tabs, deliverable detail, submission review and embedded document panels wired to the real backend module
- the deliverable detail screen no longer blocks submission creation on work-package documents; the created submission starts with `documentIds: []`, and its detail screen owns subsequent `PROJECT_DELIVERABLE_SUBMISSION` evidence uploads
- the frontend project-management shell can now differentiate experiences such as timeline-oriented and board-oriented projects by combining each project's `frameworkType` with the catalog returned by `/api/project-frameworks`; project detail includes a framework-driven `Visualizacao` route that renders Timeline, Board and Hybrid lenses over runtime milestones and deliverables without changing backend data shape
- the frontend project-management workspace now also includes `/workspace/projects/inbox`, which builds an operational review queue by aggregating `/api/projects/{projectId}/deliverables/pending-review` across the projects visible in the active context
- the frontend now also exposes `/workspace/templates/frameworks` for framework-catalog visibility, with create/edit actions restricted to internal `ADMIN` and `code` treated as immutable after creation
- the project detail participants tab now combines the existing read model with transactional add-organization and add-member flows on top of `/api/projects/{projectId}/organizations` and `/api/projects/{projectId}/members`
- the project detail milestones and deliverables tabs now include project-local create/delete actions backed by runtime project endpoints, with inline create forms, confirmation before removal and refresh of project dashboard/list data after mutations
- the project detail structure experience should add a tree panel for runtime nodes, including create child, update node and move node actions backed by `GET /api/projects/{projectId}/structure`, `POST /api/projects/{projectId}/structure/nodes`, `PATCH /api/projects/{projectId}/structure/nodes/{nodeId}` and `POST /api/projects/{projectId}/structure/nodes/{nodeId}/move`
- the organizations workspace now also exposes the audited export workflow under `/workspace/organizations/{organizationId}/exports`, backed by `GET|POST|PATCH /api/access/organizations/{organizationId}/exports`
- the current frontend create wizard now loads the authorized template catalog from `/api/project-templates`, filters by framework in the active access context and leaves template selection blank when the operator wants the backend to resolve the authorized default template
- the current frontend create wizard now validates the selected/default project template's linked structure template before submit; if the structure has no levels, the UI blocks project creation and tells the operator to create at least the root structure level first, matching the backend invariant that project structure templates must define at least one level
- the frontend now also exposes `/workspace/templates/*` for owner-admin management of project templates, structure templates, phases, milestones, deliverables and structure levels, including structure activation/deactivation and purge-protected destructive flows

## Operational validation status
- a production manual validation checklist is now tracked in `docs/runbooks/production-manual-test-checklist.md`; use it to record real production `OK`, `NOK` and `NA` evidence before moving from controlled pilot to broader sale
- the published backend environment was revalidated on `2026-04-01`: `GET /api/access/users` now returns structured `401 Unauthorized` for anonymous requests, confirming that the route is served by the secured backend controller path rather than a static-resource `404`
- `/api/auth/me`, `/api/access/users` and `/api/access/organizations` have now been validated end-to-end in dev and prod against the structured `401`/`403` contract consumed by the frontend
- the operational reaction runbook has been exercised in dev and prod for the active core administration surface
- the automatic alert thresholds are implemented, but explicit end-to-end alert-evidence capture remains a follow-up activity
- confirm the desired UX for `PARTNER` relationships in organization and user workspaces: explicit read-only distinction versus relying only on authorization failures

