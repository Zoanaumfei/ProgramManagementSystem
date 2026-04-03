CREATE TABLE IF NOT EXISTS organization_relationship (
    id VARCHAR(64) PRIMARY KEY,
    source_organization_id VARCHAR(64) NOT NULL,
    target_organization_id VARCHAR(64) NOT NULL,
    relationship_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT fk_org_relationship_source FOREIGN KEY (source_organization_id) REFERENCES organization(id),
    CONSTRAINT fk_org_relationship_target FOREIGN KEY (target_organization_id) REFERENCES organization(id),
    CONSTRAINT chk_org_relationship_type CHECK (relationship_type IN ('CUSTOMER_SUPPLIER', 'PARTNER'))
);

CREATE INDEX idx_org_relationship_source ON organization_relationship (source_organization_id);
CREATE INDEX idx_org_relationship_target ON organization_relationship (target_organization_id);
CREATE INDEX idx_org_relationship_type ON organization_relationship (relationship_type);
CREATE INDEX idx_org_relationship_status ON organization_relationship (status);

-- H2-friendly reset for tests. Production keeps the PostgreSQL block in main resources.
DELETE FROM audit_log;
DELETE FROM membership_role;
DELETE FROM user_membership;
DELETE FROM app_user;
DELETE FROM organization_relationship;
DELETE FROM organization;
DELETE FROM tenant_market;
DELETE FROM tenant;
