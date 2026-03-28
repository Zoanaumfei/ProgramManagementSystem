# Operations Governance Runbook

## Tenant rate-limit incident
Use when requests start returning `429 Too Many Requests` unexpectedly or when a shared counter store outage is suspected.

1. Confirm the affected tenant via the response `correlationId` and `path`.
2. Check whether the application is configured with `app.multitenancy.rate-limit.store=redis` in production.
3. Verify Redis availability and key churn for the `pms:tenant-rate-limit:*` prefix.
4. If Redis is unavailable, treat the issue as a production incident; do not switch to local counters in production.
5. Temporary mitigation is to restore Redis connectivity or reduce inbound request volume at the edge.

## Tenant tier change
Use when a tenant must be moved between `STANDARD` and `ENTERPRISE`, or when an internal tenant must be confirmed as `INTERNAL`.

1. Call `PATCH /api/access/tenants/{tenantId}/service-tier`.
2. Include a clear justification in the request body.
3. Prefer an `ADMIN` actor; `SUPPORT` is allowed only when audit trail and justification are present.
4. Verify the response and then confirm the new tier through `GET /api/access/tenants` or the governance service.
5. Remember that quota and rate-limit policy changes apply immediately after the transaction commits.

## Organization export execution
Use when an offboarded organization must be exported during the retention window.

1. Confirm the organization is `OFFBOARDED` and `retentionUntil` has not passed.
2. Start the export with `POST /api/access/organizations/{organizationId}/exports`.
3. Perform the manual-assisted export process.
4. Complete the export with `PATCH /api/access/organizations/{organizationId}/exports`.
5. Verify the final status with `GET /api/access/organizations/{organizationId}/exports`.
6. Preserve the audit trail justification for the full operator workflow.
