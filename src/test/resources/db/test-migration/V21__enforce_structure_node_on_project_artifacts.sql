ALTER TABLE project_milestone
    DROP CONSTRAINT uq_project_milestone_code;

ALTER TABLE project_milestone
    DROP CONSTRAINT uq_project_milestone_sequence;

ALTER TABLE project_deliverable
    DROP CONSTRAINT uq_project_deliverable_code;

ALTER TABLE project_milestone
    ALTER COLUMN structure_node_id SET NOT NULL;

ALTER TABLE project_deliverable
    ALTER COLUMN structure_node_id SET NOT NULL;

ALTER TABLE project_milestone
    ADD CONSTRAINT uq_project_milestone_node_code UNIQUE (project_id, structure_node_id, code);

ALTER TABLE project_milestone
    ADD CONSTRAINT uq_project_milestone_node_sequence UNIQUE (project_id, structure_node_id, sequence_no);

ALTER TABLE project_deliverable
    ADD CONSTRAINT uq_project_deliverable_node_code UNIQUE (project_id, structure_node_id, code);
