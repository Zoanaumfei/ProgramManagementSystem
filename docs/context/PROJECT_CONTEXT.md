# Project Context

Oryzem is being positioned as a SaaS platform for PMEs that need simple multi-tenant and multi-market administration across customer and supplier organizations.

## Active product scope
- user lifecycle
- organization relationship network
- membership-based access control
- tenant and market administration
- authentication, authorization and audit support around that core

## Backend status
The backend status below was cross-checked against the code currently present in this repository.

- the active backend surface is centered on `/api/auth/me`, `/api/access/users`, `/api/access/users/{userId}/memberships`, `/api/access/organizations`, `/api/access/organizations/{organizationId}/relationships`, `/api/access/tenants` and related operational endpoints
- the backend now also exposes the new `project-management` and `document-management` surfaces through `/api/projects`, `/api/document-contexts/{contextType}/{contextId}/documents`, `/api/documents/{documentId}/finalize-upload` and `/api/documents/{documentId}/download-url`
- the backend now also exposes project-framework catalog governance through `/api/project-frameworks`, with stable `code`, operator-facing `displayName`, `description`, `uiLayout`, lifecycle `active` and audited create/update management
- project management currently supports project creation/list/detail/update, participant organization/member registration, milestone listing, deliverable listing/detail/update, deliverable submission create/review, project dashboard and pending-review listing
- project management also exposes runtime project structure nodes through `/api/projects/{projectId}/structure`, including tree read, child-node create, node update and node move; structure-level milestones and deliverables are materialized when a matching runtime node is created
- internal `ADMIN` actors now receive a platform-wide project list from `GET /api/projects`
- the backend now exposes an explicit two-step project purge flow through `/api/projects/{projectId}/purge-intents` and `/api/projects/{projectId}/purge`
- project purge is restricted to internal `ADMIN` and `SUPPORT` actors, requires mandatory reason + final confirmation text, and physically removes project data plus linked document/storage artifacts
- project creation resolves the applied template from `templateId` when provided, and otherwise falls back to the active default template for the chosen `frameworkType`
- the dev MVP reset flow is operational through `dev-reset.cmd`, which launches a one-off ECS task inside the private-RDS VPC and clears fake/runtime data while preserving `internal-core`, `vanderson.verza@gmail.com`, baseline project frameworks, baseline project templates and baseline structure templates/levels
- document management now supports the `PROJECT`, `PROJECT_DELIVERABLE` and `PROJECT_DELIVERABLE_SUBMISSION` contexts with direct upload initiation, backend-side finalize, contextual listing, signed download URL generation and soft delete
- user onboarding now provisions the first membership during `POST /api/access/users`, so the standard invite flow no longer depends on a later bootstrap-membership step
- `GET /api/access/users/orphans` exists only as an inconsistency discovery surface for unexpected legacy/test data; orphan users are no longer treated as a valid lifecycle state
- internal break-glass administration resolves from local membership roles `ADMIN`, `SUPPORT` and `AUDITOR`, matching the Cognito bootstrap groups used for the internal global user
- user purge is now allowed for internal `ADMIN` and `SUPPORT` actors, but only for inactive users whose Cognito identity is already absent and when the authorization/audit requirements are satisfied
- the backend treats `cnpj` as the canonical organization identity inside a tenant
- organization creation no longer depends on stored hierarchy fields such as `parentOrganizationId`, `customerOrganizationId` or `hierarchyLevel`
- organization visibility is driven by explicit directed relationships, with `CUSTOMER_SUPPLIER` and `PARTNER` edges
- relationship-local organization coding now lives on `localOrganizationCode`, including create/update validation and duplicate protection per source organization
- external organization create now follows a create-or-reuse flow by `tenant + cnpj`, automatically creating or reactivating the relevant `CUSTOMER_SUPPLIER` relationship
- organization subtree delete performs offboarding first, and the explicit purge flow now clears relationship rows that reference the subtree, including inactive ones, before physical deletion
- tenant rate limiting uses a shared `TenantRateLimitCounterStore` abstraction, with Redis as the production default and local mode reserved for dev/test
- the ECS test deployment still runs the rate-limit store in local mode until a shared Redis dependency is provisioned for that environment
- tenant service tier changes flow through an audited `PATCH /api/access/tenants/{tenantId}/service-tier` endpoint and immediately affect quota and rate-limit policy resolution
- offboarded organizations now expose an audited export workflow through `/api/access/organizations/{organizationId}/exports`
- structured `401`/`403` payloads are implemented in the backend with `timestamp`, `status`, `error`, `message`, `path` and `correlationId`

## Frontend integration status
The frontend source tree is not the active implementation focus of this repository today, so the notes below should be treated as integration guidance plus tracked product status rather than fully repository-verified UI code.

- the active frontend direction remains the access-first workspace flow centered on `/workspace`, `/workspace/users`, `/workspace/organizations`, `/workspace/markets` and `/workspace/session`
- the active frontend workspace now also includes `/workspace/projects/*` for collaborative project management backed by the new project/document modules
- the frontend project-management module now ships the MVP routes for project list, project creation wizard, project detail with tabs, deliverable detail and submission detail, all integrated with the real backend contract
- the frontend project-management workspace now also exposes `/workspace/projects/inbox`, aggregating pending-review deliverables across the visible project portfolio with filters for current-context assignment, overdue work and project-risk prioritization
- the frontend project and template workspaces now resolve framework presentation from the backend `GET /api/project-frameworks` catalog instead of a closed client-side enum, using `displayName` for operator labels, `uiLayout` for visual-mode hints and only active frameworks in creation flows while preserving historical framework codes in read models
- the frontend project list now supports the internal administrative global-view behavior from `GET /api/projects`, surfacing organizational context per project and warning when the listed project belongs to a tenant different from the operator active tenant
- the frontend project-management workspace now also exposes the explicit two-step project purge flow for internal `ADMIN` and `SUPPORT` operators, requiring reason + backend intent + exact `PURGE PROJECT` confirmation before the irreversible delete is executed
- the project detail screen now centralizes summary, milestones, deliverables, participants and project documents, while the deliverable detail screen concentrates work-package documents plus submission creation and the submission detail screen concentrates review/approve/reject actions
- the frontend project detail screen should expose a runtime structure tab backed by `/api/projects/{projectId}/structure`, support create/update/move operations for project structure nodes, avoid client-side `levelTemplateId` selection during node creation, and scope dashboard, milestones, deliverables and pending-review data with `structureNodeId` when a node is selected
- the project detail structure scope now treats authorization failures from scoped dashboard/milestone/deliverable/pending-review refreshes as local scope feedback instead of a full-page project failure, while translating the backend project-operation refusal into operator-facing Portuguese copy with endpoint and correlationId
- the project create flow now binds the lead organization to the operator's active access context and resolves the selectable template catalog from the backend per active organization context, falling back to the authorized default template when no explicit template is chosen
- the frontend now also exposes `/workspace/templates/*` as an admin-only template-management surface for `project templates`, `project structure templates`, `phases`, `milestones`, `deliverables` and `structure levels`, with list/detail scoped by the active organization context and owner-aware backend authorization for activate/deactivate and purge flows
- the project-template milestone editor in `/workspace/templates/project-templates/:templateId` now includes front-end-only contextual help triggers (`?`) beside each field label, opening short operator guidance after about 2 seconds of hover and closing when the pointer leaves, without changing the backend contract; this is the pilot UX for expanding interactive field manuals to other template forms
- the template-management frontend now passes the active `membershipId` explicitly as `X-Access-Context` across project-template and project-structure-template queries/mutations, reducing admin-operation failures caused by relying only on session-stored context during template and structure-level administration
- the templates workspace now also exposes `/workspace/templates/frameworks` for framework-catalog visibility, while create/edit actions remain limited to internal `ADMIN` and keep `code` immutable after creation
- the project detail participants tab now combines the existing read model with transactional additions for participant organizations and project members using `/api/projects/{projectId}/organizations` and `/api/projects/{projectId}/members`
- the project milestones tab now supports direct updates for planned/actual dates, owner organization and status using the backend `version` field
- the organizations workspace is now split into dedicated route-driven screens under `/workspace/organizations/*` for list, detail, create, edit, relationships and purge flows
- the organizations detail screen now uses a dashboard-style two-column layout with identity/metadata on the left and actions/relationship preview on the right
- the organizations relationships screen now uses a hub layout with a contextual overview rail, a dedicated creation panel and a relationship list/editor area
- the organizations workspace render was further decomposed into dedicated screen components for list, create/edit, purge, detail and relationships, leaving the main container focused on routing and shared state
- the organizations screen split was validated with a production build after extraction, so the routing and component boundaries are now the current stable layout baseline
- the organizations workspace now also exposes an audited export workflow route under `/workspace/organizations/{organizationId}/exports`, with operator-facing status refresh plus start/complete transitions backed by `/api/access/organizations/{organizationId}/exports`
- the active frontend workspace now also includes `/workspace/operations` for operational visibility over `429`, `409`, offboarding and export workflow
- the operational dashboard is backed by `GET /api/admin/operational/overview`, which returns aggregated KPIs, series, top tenants, tenant drill-down details, alerts and recent events
- the minimum operational dashboard is now running in both dev and prod frontend environments with the expected panels for `429`, `409` quota, offboarding and export requested/completed
- the operational dashboard frontend now also includes the audited tenant-governance control for `PATCH /api/access/tenants/{tenantId}/service-tier`, allowing authorized internal operators to submit service-tier changes with justification and immediate backend refresh
- automatic alert thresholds for rate-limit spike, quota spike and export backlog are implemented, although explicit end-to-end alert testing is still tracked separately from the shipped dashboard
- the users experience should consume `/api/access/users` and membership APIs as the primary access-management surface
- the frontend users workspace now separates identity create/edit flows from the main administration list: `/workspace/users` stays focused on listing, support actions, access management and repair views, while `/workspace/users/new` and `/workspace/users/{userId}/edit` host the dedicated create and edit forms
- the users workspace may expose a dedicated `Usuarios sem membership` diagnostic view backed by `/api/access/users/orphans`, explicitly positioned as data-repair tooling rather than the normal onboarding path
- user creation in the frontend should require `organizationId`, optional `marketId` and non-empty `roles` so the first membership is provisioned in the same request
- the user-create form in `/workspace/users` now requires an explicit organization selection by the operator, shows an inline warning while none is selected and blocks submit even if the create button state is bypassed
- the frontend users workspace now surfaces orphan-user and missing-active-membership failures as operational inconsistency states, not as generic request errors, and includes `correlationId` in the administrative error copy when the backend provides it
- the memberships workspace copy now treats `bootstrap-membership` as an exceptional repair flow only; normal onboarding messaging points to `POST /api/access/users` as the standard path
- organization management in the frontend should follow the relationship-first model and treat `localOrganizationCode` as relationship metadata, not organization identity
- frontend flows should stop depending on legacy hierarchy fields and should not imply a single canonical relationship breadcrumb
- frontend error handling should surface backend-provided `401`/`403` messages and `correlationId` values consistently
- the frontend admin rollout for structured `401`/`403` is now implemented in the active frontend workspace: `/workspace/users`, `/workspace/organizations` and `/workspace/session` preserve backend `message`, `path` and `correlationId` instead of collapsing those cases into generic errors
- frontend regression coverage now includes structured security-error handling for `/api/auth/me`, `/api/access/users` and `/api/access/organizations`, including visible `correlationId` assertions in the admin UI
- frontend environment resolution now auto-upgrades an insecure `http` API base URL to `https` whenever the app itself is running under an `https` origin, reducing published mixed-content failures during auth/login
- browser-blocked auth requests now surface an actionable network/CORS/mixed-content hint from the shared API client instead of only bubbling a generic `Failed to fetch`-style error
- browser-blocked frontend API requests now also expose the resolved request URL and the current frontend origin in the shared `ApiError`, making dev diagnostics faster when a local env override or proxy mismatch silently points away from `https://api.oryzem.com`
- actionable UI elements now use the hand cursor globally (`button`, `a`, `summary`, `label`, `select`, and checkbox/radio inputs), while disabled buttons keep the normal visual weight instead of fading and the login submit button can still switch to `wait` only during the active sign-in request
- the login page now syncs browser autofill values from the DOM on mount so the submit button reflects the real credential state on first visit instead of staying visually disabled until a manual input event
- the login page also watches for late browser autofill updates during the first seconds after mount and via autofill animation hooks, so the submit button can enable itself without requiring a focus/click round-trip
- the login submit button now stays enabled before the request starts and rejects empty credential submits inside the handler, which keeps the hand cursor visible on first visit even if the browser has not exposed autofill yet
- the login page has been reframed as a focused product entry screen: the credential form is the primary surface, first-access/reset guidance is visible near the action, environment/configuration diagnostics are collapsed into connection details, and empty credential submits now show inline feedback instead of failing silently
- the frontend visual system has started its production-readiness pass: global typography, surface colors, card/control radius, AppShell hierarchy, authenticated session context, workspace entry cards and module-level panel/list styling were simplified to reduce development-console feel while preserving the existing backend contracts
- the frontend production-readiness UX pass now covers the main authenticated areas screen by screen: login/first-access/reset, workspace, projects, organizations, users/access repair, markets, operations, template management and framework catalog copy were reframed around operator tasks, with technical terms such as backend, request, bootstrap, purge and raw template nouns reduced from primary UI surfaces while preserving the underlying contracts
- the production frontend should point to `https://api.oryzem.com` as the canonical API base URL
- the published backend environment was revalidated on `2026-04-01`: `GET /api/access/users` now resolves through the secured backend route and returns structured `401 Unauthorized` for anonymous traffic instead of the previously reported static-resource-style `404`
- the structured `401`/`403` behavior for `/api/auth/me`, `/api/access/users` and `/api/access/organizations` has now been validated end-to-end in both dev and prod, and the operational reaction runbook has also been exercised in those environments

## Core themes
- user and organization administration as the release anchor
- contextual access control by tenant, organization and market
- low-friction administration suitable for smaller and medium-sized businesses
- clear separation between identity data and runtime authorization context

