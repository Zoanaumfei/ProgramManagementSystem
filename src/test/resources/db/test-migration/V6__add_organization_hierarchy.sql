ALTER TABLE organization
    ADD COLUMN tenant_type VARCHAR(32);

ALTER TABLE organization
    ADD COLUMN parent_organization_id VARCHAR(64);

ALTER TABLE organization
    ADD COLUMN customer_organization_id VARCHAR(64);

ALTER TABLE organization
    ADD COLUMN hierarchy_level INTEGER;

UPDATE organization
SET tenant_type = CASE
    WHEN id = 'internal-core' THEN 'INTERNAL'
    ELSE 'EXTERNAL'
END
WHERE tenant_type IS NULL;

UPDATE organization
SET customer_organization_id = CASE
    WHEN tenant_type = 'EXTERNAL' THEN id
    ELSE NULL
END
WHERE customer_organization_id IS NULL;

UPDATE organization
SET hierarchy_level = 0
WHERE hierarchy_level IS NULL;

ALTER TABLE organization
    ALTER COLUMN tenant_type SET NOT NULL;

ALTER TABLE organization
    ALTER COLUMN hierarchy_level SET NOT NULL;

ALTER TABLE organization
    ADD CONSTRAINT fk_organization_parent
        FOREIGN KEY (parent_organization_id) REFERENCES organization(id);

ALTER TABLE organization
    ADD CONSTRAINT fk_organization_customer
        FOREIGN KEY (customer_organization_id) REFERENCES organization(id);

CREATE INDEX idx_organization_parent ON organization (parent_organization_id);
CREATE INDEX idx_organization_customer ON organization (customer_organization_id);
CREATE INDEX idx_organization_tenant_type ON organization (tenant_type);
