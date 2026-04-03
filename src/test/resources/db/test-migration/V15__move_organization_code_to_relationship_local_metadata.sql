ALTER TABLE organization_relationship
    ADD COLUMN local_organization_code VARCHAR(80);

UPDATE organization_relationship relationship
SET local_organization_code = (
    SELECT organization.code
    FROM organization
    WHERE organization.id = relationship.target_organization_id
)
WHERE relationship.local_organization_code IS NULL
  AND relationship.target_organization_id IN (SELECT id FROM organization WHERE code IS NOT NULL);

CREATE UNIQUE INDEX uq_org_relationship_source_local_code
    ON organization_relationship (source_organization_id, local_organization_code);

ALTER TABLE organization
    DROP COLUMN IF EXISTS code;
