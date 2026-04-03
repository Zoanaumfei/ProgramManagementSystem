ALTER TABLE organization_relationship
    ADD COLUMN local_organization_code VARCHAR(80);

UPDATE organization_relationship relationship
SET local_organization_code = organization.code
FROM organization
WHERE relationship.target_organization_id = organization.id
  AND relationship.local_organization_code IS NULL
  AND organization.code IS NOT NULL;

CREATE UNIQUE INDEX uq_org_relationship_source_local_code
    ON organization_relationship (source_organization_id, local_organization_code);

ALTER TABLE organization
    DROP CONSTRAINT IF EXISTS organization_code_key;

ALTER TABLE organization
    DROP COLUMN IF EXISTS code;
