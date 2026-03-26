# Cross-Tenant Incident Runbook

## Goal
Contain, assess and close any suspected cross-tenant read or write exposure in the active core (`/api/auth/me`, `/api/access/*`).

## Detection triggers
- unexpected user or organization data visible outside the actor tenant hierarchy
- audit events flagged as `crossTenant=true` without an approved support override
- support reports of wrong active tenant or wrong active organization after context switching
- spikes of `403`, `409` or `429` that line up with suspected tenant-boundary regressions

## Immediate containment
1. Identify the affected tenant ids, organization ids, user ids and correlation ids.
2. Disable any operator workflow that is actively reproducing the issue.
3. If the incident is tied to a specific organization subtree, offboard the subtree or the affected memberships to revoke access fast.
4. If the issue is systemic, pause rollout or scale down the faulty deployment slice.
5. Preserve logs, audit rows and request samples before cleanup.

## Investigation checklist
- confirm the exact route, method and actor membership used
- inspect `audit_log` entries around the correlation id
- verify the actor `membershipId`, `activeTenantId` and `activeOrganizationId` via `/api/auth/me`
- confirm whether `X-Access-Context` was supplied and whether the value was valid
- verify whether a support override and justification were present
- compare expected organization subtree with returned records
- confirm whether the request hit quota or rate-limit defenses before the suspected leak path

## SQL probes
```sql
select id, event_type, actor_user_id, actor_tenant_id, target_tenant_id, cross_tenant, correlation_id, created_at
from audit_log
where correlation_id = :correlation_id
order by created_at asc;
```

```sql
select id, user_id, tenant_id, organization_id, status, lifecycle_state, is_default, updated_at
from user_membership
where user_id = :user_id
order by updated_at desc;
```

```sql
select id, tenant_id, status, lifecycle_state, retention_until, data_export_status
from organization
where id = :organization_id;
```

## Recovery actions
- if the issue is caused by a leaked membership, offboard the affected membership and verify the user still has only valid memberships
- if the issue is caused by organization visibility, compare the returned organization ids against the actor subtree and patch the filter path before re-enabling traffic
- if data was modified cross-tenant, isolate impacted records, audit all writes under the same actor/correlation window and restore from authoritative state when needed
- if the defect came from a recent deploy, roll back and keep the failing artifact unavailable until regression tests are green

## Validation before closure
- reproduce the original request and verify it now returns the expected `403`, `400`, `409` or tenant-scoped data only
- confirm audit trail coverage for the relevant critical operation
- confirm no additional affected tenants or organizations remain in the same time window
- capture root cause, scope, containment time and follow-up action items in the incident record

## Follow-up requirements
- add or update automated tests for the exact leak pattern
- document any missing guardrail in `docs/context/OPEN_GAPS.md`
- if the issue involved a public contract ambiguity, update `docs/context/API_CONTRACT.md`
