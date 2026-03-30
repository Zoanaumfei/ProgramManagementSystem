CREATE TABLE organization_relationship (
    id VARCHAR(64) PRIMARY KEY,
    source_organization_id VARCHAR(64) NOT NULL REFERENCES organization(id),
    target_organization_id VARCHAR(64) NOT NULL REFERENCES organization(id),
    relationship_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE INDEX idx_org_relationship_source ON organization_relationship (source_organization_id);
CREATE INDEX idx_org_relationship_target ON organization_relationship (target_organization_id);
CREATE INDEX idx_org_relationship_type ON organization_relationship (relationship_type);
CREATE INDEX idx_org_relationship_status ON organization_relationship (status);

-- Wipe existing domain data to allow the new access model to bootstrap cleanly.
DO $$
DECLARE
    table_name text;
    table_list text[] := ARRAY[
        'deliverable_document',
        'deliverable',
        'item_record',
        'product_record',
        'project_milestone',
        'open_issue',
        'project_record',
        'program_participation',
        'program_record',
        'milestone_template_item',
        'milestone_template',
        'operation_record',
        'audit_log',
        'membership_role',
        'user_membership',
        'app_user',
        'organization_relationship',
        'tenant_market',
        'tenant',
        'organization'
    ];
BEGIN
    FOREACH table_name IN ARRAY table_list LOOP
        IF to_regclass(table_name) IS NOT NULL THEN
            EXECUTE format('TRUNCATE TABLE %I CASCADE', table_name);
        END IF;
    END LOOP;
END $$;
