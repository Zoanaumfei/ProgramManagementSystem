ALTER TABLE organization
    ADD COLUMN cnpj VARCHAR(14);

ALTER TABLE organization
    DROP CONSTRAINT IF EXISTS fk_organization_parent;

ALTER TABLE organization
    DROP CONSTRAINT IF EXISTS fk_organization_customer;

DROP INDEX IF EXISTS idx_organization_parent;
DROP INDEX IF EXISTS idx_organization_customer;

ALTER TABLE organization
    DROP COLUMN IF EXISTS parent_organization_id;

ALTER TABLE organization
    DROP COLUMN IF EXISTS customer_organization_id;

ALTER TABLE organization
    DROP COLUMN IF EXISTS hierarchy_level;

CREATE UNIQUE INDEX uq_organization_tenant_cnpj ON organization (tenant_id, cnpj);
