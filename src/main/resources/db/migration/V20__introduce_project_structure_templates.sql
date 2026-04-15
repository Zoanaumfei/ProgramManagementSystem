CREATE TABLE project_structure_template (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    framework_type VARCHAR(32) NOT NULL,
    version INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_project_structure_template_framework_name_version UNIQUE (framework_type, name, version)
);

CREATE TABLE project_structure_level_template (
    id VARCHAR(64) PRIMARY KEY,
    structure_template_id VARCHAR(64) NOT NULL REFERENCES project_structure_template(id) ON DELETE CASCADE,
    sequence_no INTEGER NOT NULL,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(64) NOT NULL,
    allows_children BOOLEAN NOT NULL,
    allows_milestones BOOLEAN NOT NULL,
    allows_deliverables BOOLEAN NOT NULL,
    CONSTRAINT uq_project_structure_level_template_sequence UNIQUE (structure_template_id, sequence_no),
    CONSTRAINT uq_project_structure_level_template_code UNIQUE (structure_template_id, code)
);

CREATE TABLE project_structure_node (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    level_template_id VARCHAR(64) NOT NULL REFERENCES project_structure_level_template(id),
    parent_node_id VARCHAR(64) REFERENCES project_structure_node(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(64) NOT NULL,
    sequence_no INTEGER NOT NULL,
    owner_organization_id VARCHAR(64),
    responsible_user_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    visibility_scope VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_project_structure_node_parent_code UNIQUE (project_id, parent_node_id, code)
);

ALTER TABLE project_template
    ADD COLUMN structure_template_id VARCHAR(64);

ALTER TABLE project_template
    ADD CONSTRAINT fk_project_template_structure_template
        FOREIGN KEY (structure_template_id) REFERENCES project_structure_template(id);

ALTER TABLE project_milestone_template
    ADD COLUMN applies_to_type VARCHAR(32) NOT NULL DEFAULT 'ROOT_NODE';

ALTER TABLE project_milestone_template
    ADD COLUMN structure_level_template_id VARCHAR(64);

ALTER TABLE project_milestone_template
    ADD CONSTRAINT fk_project_milestone_template_structure_level
        FOREIGN KEY (structure_level_template_id) REFERENCES project_structure_level_template(id) ON DELETE SET NULL;

ALTER TABLE deliverable_template
    ADD COLUMN applies_to_type VARCHAR(32) NOT NULL DEFAULT 'ROOT_NODE';

ALTER TABLE deliverable_template
    ADD COLUMN structure_level_template_id VARCHAR(64);

ALTER TABLE deliverable_template
    ADD CONSTRAINT fk_deliverable_template_structure_level
        FOREIGN KEY (structure_level_template_id) REFERENCES project_structure_level_template(id) ON DELETE SET NULL;

ALTER TABLE project_milestone
    ADD COLUMN structure_node_id VARCHAR(64);

ALTER TABLE project_milestone
    ADD CONSTRAINT fk_project_milestone_structure_node
        FOREIGN KEY (structure_node_id) REFERENCES project_structure_node(id) ON DELETE SET NULL;

ALTER TABLE project_deliverable
    ADD COLUMN structure_node_id VARCHAR(64);

ALTER TABLE project_deliverable
    ADD CONSTRAINT fk_project_deliverable_structure_node
        FOREIGN KEY (structure_node_id) REFERENCES project_structure_node(id) ON DELETE SET NULL;

INSERT INTO project_structure_template (id, name, framework_type, version, active, created_at) VALUES
    ('PST-APQP-V1', 'APQP Default Structure v1', 'APQP', 1, TRUE, TIMESTAMP WITH TIME ZONE '2026-04-08 00:00:00+00'),
    ('PST-VDA-MLA-V1', 'VDA MLA Default Structure v1', 'VDA_MLA', 1, TRUE, TIMESTAMP WITH TIME ZONE '2026-04-08 00:00:00+00'),
    ('PST-CUSTOM-V1', 'Custom Default Structure v1', 'CUSTOM', 1, TRUE, TIMESTAMP WITH TIME ZONE '2026-04-08 00:00:00+00');

INSERT INTO project_structure_level_template (id, structure_template_id, sequence_no, name, code, allows_children, allows_milestones, allows_deliverables) VALUES
    ('PSLT-APQP-001', 'PST-APQP-V1', 1, 'Project', 'PROJECT', FALSE, TRUE, TRUE),
    ('PSLT-VDA-001', 'PST-VDA-MLA-V1', 1, 'Project', 'PROJECT', FALSE, TRUE, TRUE),
    ('PSLT-CUS-001', 'PST-CUSTOM-V1', 1, 'Project', 'PROJECT', FALSE, TRUE, TRUE);

UPDATE project_template
SET structure_template_id = CASE id
    WHEN 'TMP-APQP-V1' THEN 'PST-APQP-V1'
    WHEN 'TMP-VDA-MLA-V1' THEN 'PST-VDA-MLA-V1'
    WHEN 'TMP-CUSTOM-V1' THEN 'PST-CUSTOM-V1'
    ELSE structure_template_id
END;

UPDATE project_milestone_template
SET applies_to_type = 'ROOT_NODE',
    structure_level_template_id = CASE template_id
        WHEN 'TMP-APQP-V1' THEN 'PSLT-APQP-001'
        WHEN 'TMP-VDA-MLA-V1' THEN 'PSLT-VDA-001'
        WHEN 'TMP-CUSTOM-V1' THEN 'PSLT-CUS-001'
        ELSE structure_level_template_id
    END;

UPDATE deliverable_template
SET applies_to_type = 'ROOT_NODE',
    structure_level_template_id = CASE template_id
        WHEN 'TMP-APQP-V1' THEN 'PSLT-APQP-001'
        WHEN 'TMP-VDA-MLA-V1' THEN 'PSLT-VDA-001'
        WHEN 'TMP-CUSTOM-V1' THEN 'PSLT-CUS-001'
        ELSE structure_level_template_id
    END;

INSERT INTO project_structure_node (
    id,
    project_id,
    level_template_id,
    parent_node_id,
    name,
    code,
    sequence_no,
    owner_organization_id,
    responsible_user_id,
    status,
    visibility_scope,
    version
)
SELECT
    p.id || '-ROOT' AS id,
    p.id,
    CASE pt.structure_template_id
        WHEN 'PST-APQP-V1' THEN 'PSLT-APQP-001'
        WHEN 'PST-VDA-MLA-V1' THEN 'PSLT-VDA-001'
        WHEN 'PST-CUSTOM-V1' THEN 'PSLT-CUS-001'
    END AS level_template_id,
    NULL,
    p.name,
    p.code,
    1,
    p.lead_organization_id,
    p.created_by_user_id,
    CASE p.status
        WHEN 'ACTIVE' THEN 'ACTIVE'
        WHEN 'COMPLETED' THEN 'COMPLETED'
        WHEN 'CANCELLED' THEN 'CANCELLED'
        ELSE 'PLANNED'
    END,
    p.visibility_scope,
    0
FROM project p
JOIN project_template pt ON pt.id = p.template_id;

UPDATE project_milestone
SET structure_node_id = project_id || '-ROOT'
WHERE structure_node_id IS NULL;

UPDATE project_deliverable
SET structure_node_id = project_id || '-ROOT'
WHERE structure_node_id IS NULL;

ALTER TABLE project_template
    ALTER COLUMN structure_template_id SET NOT NULL;

CREATE INDEX idx_project_structure_level_template_structure_sequence
    ON project_structure_level_template (structure_template_id, sequence_no);

CREATE INDEX idx_project_structure_node_project_parent_sequence
    ON project_structure_node (project_id, parent_node_id, sequence_no);

CREATE INDEX idx_project_structure_node_project_level
    ON project_structure_node (project_id, level_template_id);

CREATE INDEX idx_project_milestone_project_structure_node
    ON project_milestone (project_id, structure_node_id);

CREATE INDEX idx_project_deliverable_project_structure_node
    ON project_deliverable (project_id, structure_node_id);
