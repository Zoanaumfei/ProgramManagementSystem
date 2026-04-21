ALTER TABLE project_template
    ADD COLUMN owner_organization_id VARCHAR(64);

ALTER TABLE project_structure_template
    ADD COLUMN owner_organization_id VARCHAR(64);

UPDATE project_template template
SET owner_organization_id = COALESCE(
    (
        SELECT t.root_organization_id
        FROM tenant t
        WHERE t.root_organization_id IS NOT NULL
          AND t.tenant_type = 'EXTERNAL'
        ORDER BY t.created_at ASC
        LIMIT 1
    ),
    (
        SELECT o.id
        FROM organization o
        ORDER BY o.created_at ASC
        LIMIT 1
    ),
    'tenant-a'
)
WHERE template.owner_organization_id IS NULL;

UPDATE project_structure_template structure_template
SET owner_organization_id = COALESCE(
    (
        SELECT t.root_organization_id
        FROM tenant t
        WHERE t.root_organization_id IS NOT NULL
          AND t.tenant_type = 'EXTERNAL'
        ORDER BY t.created_at ASC
        LIMIT 1
    ),
    (
        SELECT o.id
        FROM organization o
        ORDER BY o.created_at ASC
        LIMIT 1
    ),
    'tenant-a'
)
WHERE structure_template.owner_organization_id IS NULL;

ALTER TABLE project_template
    ALTER COLUMN owner_organization_id SET NOT NULL;

ALTER TABLE project_structure_template
    ALTER COLUMN owner_organization_id SET NOT NULL;

CREATE INDEX idx_project_template_owner_organization
    ON project_template (owner_organization_id);

CREATE INDEX idx_project_structure_template_owner_organization
    ON project_structure_template (owner_organization_id);
