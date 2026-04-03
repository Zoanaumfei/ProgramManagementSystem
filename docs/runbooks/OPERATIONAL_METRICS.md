# Operational Metrics

Use these actuator metrics as the backend source for the minimum operational dashboards and alerts in Frente A.

## HTTP operational responses
- metric: `oryzem.operational.http.responses`
- type: counter
- tags:
  - `tenantId`
  - `tenantTier`
  - `path`
  - `status`
  - `category`

### Expected categories
- `rate_limit`
  - emitted when tenant-scoped throttling returns `429`
- `quota_organizations`
  - emitted when organization quota returns `409`
- `quota_markets`
  - emitted when market quota returns `409`
- `quota_active_memberships`
  - emitted when active-membership quota returns `409`

## Tenant operational events
- metric: `oryzem.operational.events`
- type: counter
- tags:
  - `eventType`
  - `tenantId`

### Expected event types
- `organization_offboard`
- `organization_export_requested`
- `organization_export_completed`

## Dashboard starter queries
- `429` rate-limit:
  - filter `metric=oryzem.operational.http.responses`, `status=429`, `category=rate_limit`
- `409` quota:
  - filter `metric=oryzem.operational.http.responses`, `status=409`, `category=quota_*`
- offboarding/export by tenant:
  - filter `metric=oryzem.operational.events`

## Alert starter rules
- alert when `status=429` and `category=rate_limit` spikes for the same `tenantId`
- alert when `status=409` and `category=quota_*` repeats for the same `tenantId`
- alert when `organization_export_requested` grows without a matching `organization_export_completed` in the expected operating window
