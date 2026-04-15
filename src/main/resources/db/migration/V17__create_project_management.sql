CREATE TABLE project_template (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    framework_type VARCHAR(32) NOT NULL,
    version INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    is_default BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_project_template_framework_version UNIQUE (framework_type, version)
);

CREATE TABLE project_phase_template (
    id VARCHAR(64) PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL REFERENCES project_template(id) ON DELETE CASCADE,
    sequence_no INTEGER NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    planned_start_offset_days INTEGER,
    planned_end_offset_days INTEGER NOT NULL,
    CONSTRAINT uq_project_phase_template_sequence UNIQUE (template_id, sequence_no)
);

CREATE TABLE project_milestone_template (
    id VARCHAR(64) PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL REFERENCES project_template(id) ON DELETE CASCADE,
    phase_template_id VARCHAR(64) REFERENCES project_phase_template(id) ON DELETE SET NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    sequence_no INTEGER NOT NULL,
    description TEXT,
    planned_offset_days INTEGER NOT NULL,
    owner_organization_role VARCHAR(32),
    visibility_scope VARCHAR(32) NOT NULL,
    CONSTRAINT uq_project_milestone_template_code UNIQUE (template_id, code),
    CONSTRAINT uq_project_milestone_template_sequence UNIQUE (template_id, sequence_no)
);

CREATE TABLE deliverable_template (
    id VARCHAR(64) PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL REFERENCES project_template(id) ON DELETE CASCADE,
    phase_template_id VARCHAR(64) REFERENCES project_phase_template(id) ON DELETE SET NULL,
    milestone_template_id VARCHAR(64) REFERENCES project_milestone_template(id) ON DELETE SET NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    deliverable_type VARCHAR(32) NOT NULL,
    required_document BOOLEAN NOT NULL,
    planned_due_offset_days INTEGER NOT NULL,
    responsible_organization_role VARCHAR(32),
    approver_organization_role VARCHAR(32),
    visibility_scope VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    CONSTRAINT uq_deliverable_template_code UNIQUE (template_id, code)
);

CREATE TABLE project (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    framework_type VARCHAR(32) NOT NULL,
    template_id VARCHAR(64) NOT NULL REFERENCES project_template(id),
    template_version INTEGER NOT NULL,
    lead_organization_id VARCHAR(64) NOT NULL,
    customer_organization_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    visibility_scope VARCHAR(32) NOT NULL,
    planned_start_date DATE,
    planned_end_date DATE,
    actual_start_date DATE,
    actual_end_date DATE,
    created_by_user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_project_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE project_organization (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    organization_id VARCHAR(64) NOT NULL,
    role_type VARCHAR(32) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL,
    CONSTRAINT uq_project_organization UNIQUE (project_id, organization_id)
);

CREATE TABLE project_member (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL,
    organization_id VARCHAR(64) NOT NULL,
    project_role VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_project_member UNIQUE (project_id, user_id)
);

CREATE TABLE project_phase (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    sequence_no INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    planned_start_date DATE,
    planned_end_date DATE,
    actual_start_date DATE,
    actual_end_date DATE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_project_phase_sequence UNIQUE (project_id, sequence_no)
);

CREATE TABLE project_milestone (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    phase_id VARCHAR(64) REFERENCES project_phase(id) ON DELETE SET NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    sequence_no INTEGER NOT NULL,
    planned_date DATE,
    actual_date DATE,
    status VARCHAR(32) NOT NULL,
    owner_organization_id VARCHAR(64),
    visibility_scope VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_project_milestone_code UNIQUE (project_id, code),
    CONSTRAINT uq_project_milestone_sequence UNIQUE (project_id, sequence_no)
);

CREATE TABLE project_deliverable (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    phase_id VARCHAR(64) REFERENCES project_phase(id) ON DELETE SET NULL,
    milestone_id VARCHAR(64) REFERENCES project_milestone(id) ON DELETE SET NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    deliverable_type VARCHAR(32) NOT NULL,
    responsible_organization_id VARCHAR(64),
    responsible_user_id VARCHAR(64),
    approver_organization_id VARCHAR(64),
    approver_user_id VARCHAR(64),
    required_document BOOLEAN NOT NULL,
    planned_due_date DATE,
    submitted_at TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    visibility_scope VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_project_deliverable_code UNIQUE (project_id, code)
);

CREATE TABLE deliverable_submission (
    id VARCHAR(64) PRIMARY KEY,
    deliverable_id VARCHAR(64) NOT NULL REFERENCES project_deliverable(id) ON DELETE CASCADE,
    submission_number INTEGER NOT NULL,
    submitted_by_user_id VARCHAR(64) NOT NULL,
    submitted_by_organization_id VARCHAR(64) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    review_comment TEXT,
    reviewed_by_user_id VARCHAR(64),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_deliverable_submission_number UNIQUE (deliverable_id, submission_number)
);

CREATE TABLE deliverable_submission_document (
    id VARCHAR(64) PRIMARY KEY,
    submission_id VARCHAR(64) NOT NULL REFERENCES deliverable_submission(id) ON DELETE CASCADE,
    document_id VARCHAR(64) NOT NULL,
    CONSTRAINT uq_deliverable_submission_document UNIQUE (submission_id, document_id)
);

CREATE TABLE project_idempotency (
    idempotency_key VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    operation VARCHAR(64) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (tenant_id, operation, idempotency_key)
);

CREATE INDEX idx_project_tenant_status ON project (tenant_id, status);
CREATE INDEX idx_project_created_at ON project (created_at);
CREATE INDEX idx_project_organization_project_org ON project_organization (project_id, organization_id);
CREATE INDEX idx_project_member_project_user ON project_member (project_id, user_id);
CREATE INDEX idx_project_milestone_project_status_date ON project_milestone (project_id, status, planned_date);
CREATE INDEX idx_project_deliverable_project_status_due ON project_deliverable (project_id, status, planned_due_date);
CREATE INDEX idx_project_deliverable_responsible ON project_deliverable (responsible_organization_id, responsible_user_id);
CREATE INDEX idx_deliverable_submission_deliverable_status_submitted ON deliverable_submission (deliverable_id, status, submitted_at);
CREATE INDEX idx_deliverable_submission_document_submission_document ON deliverable_submission_document (submission_id, document_id);

INSERT INTO project_template (id, name, framework_type, version, status, is_default, created_at) VALUES
    ('TMP-APQP-V1', 'APQP Default v1', 'APQP', 1, 'ACTIVE', TRUE, TIMESTAMP WITH TIME ZONE '2026-04-08 00:00:00+00'),
    ('TMP-VDA-MLA-V1', 'VDA MLA Default v1', 'VDA_MLA', 1, 'ACTIVE', TRUE, TIMESTAMP WITH TIME ZONE '2026-04-08 00:00:00+00'),
    ('TMP-CUSTOM-V1', 'Custom Default v1', 'CUSTOM', 1, 'ACTIVE', TRUE, TIMESTAMP WITH TIME ZONE '2026-04-08 00:00:00+00');

INSERT INTO project_phase_template (id, template_id, sequence_no, name, description, planned_start_offset_days, planned_end_offset_days) VALUES
    ('PHT-APQP-001', 'TMP-APQP-V1', 1, 'Planning', 'Project planning and kickoff.', 0, 20),
    ('PHT-APQP-002', 'TMP-APQP-V1', 2, 'Product Development', 'Engineering and validation preparation.', 21, 55),
    ('PHT-APQP-003', 'TMP-APQP-V1', 3, 'Process Validation', 'Readiness and final release activities.', 56, 90),
    ('PHT-VDA-001', 'TMP-VDA-MLA-V1', 1, 'Prepare', 'Preparation and assessment scope.', 0, 30),
    ('PHT-VDA-002', 'TMP-VDA-MLA-V1', 2, 'Implement', 'Execute actions and evidence collection.', 31, 65),
    ('PHT-VDA-003', 'TMP-VDA-MLA-V1', 3, 'Validate', 'Final validation and closure.', 66, 95),
    ('PHT-CUS-001', 'TMP-CUSTOM-V1', 1, 'Execution', 'Generic execution phase.', 0, 45);

INSERT INTO project_milestone_template (id, template_id, phase_template_id, code, name, sequence_no, description, planned_offset_days, owner_organization_role, visibility_scope) VALUES
    ('PMT-APQP-001', 'TMP-APQP-V1', 'PHT-APQP-001', 'APQP_GATE_1', 'Kickoff Approved', 1, 'Kickoff and scope aligned.', 5, 'LEAD', 'ALL_PROJECT_PARTICIPANTS'),
    ('PMT-APQP-002', 'TMP-APQP-V1', 'PHT-APQP-002', 'APQP_GATE_2', 'Design Review', 2, 'Design package reviewed.', 40, 'LEAD', 'ALL_PROJECT_PARTICIPANTS'),
    ('PMT-APQP-003', 'TMP-APQP-V1', 'PHT-APQP-003', 'APQP_GATE_3', 'Process Release', 3, 'Process readiness gate.', 85, 'SUPPLIER', 'ALL_PROJECT_PARTICIPANTS'),
    ('PMT-VDA-001', 'TMP-VDA-MLA-V1', 'PHT-VDA-001', 'MLA_GATE_1', 'Assessment Baseline', 1, 'Baseline agreed.', 10, 'LEAD', 'ALL_PROJECT_PARTICIPANTS'),
    ('PMT-VDA-002', 'TMP-VDA-MLA-V1', 'PHT-VDA-002', 'MLA_GATE_2', 'Action Review', 2, 'Action plan reviewed.', 55, 'SUPPLIER', 'ALL_PROJECT_PARTICIPANTS'),
    ('PMT-VDA-003', 'TMP-VDA-MLA-V1', 'PHT-VDA-003', 'MLA_GATE_3', 'Closure Review', 3, 'Final closure review.', 90, 'CUSTOMER', 'ALL_PROJECT_PARTICIPANTS'),
    ('PMT-CUS-001', 'TMP-CUSTOM-V1', 'PHT-CUS-001', 'CUSTOM_GATE_1', 'Custom Gate', 1, 'Generic project gate.', 30, 'LEAD', 'ALL_PROJECT_PARTICIPANTS');

INSERT INTO deliverable_template (id, template_id, phase_template_id, milestone_template_id, code, name, description, deliverable_type, required_document, planned_due_offset_days, responsible_organization_role, approver_organization_role, visibility_scope, priority) VALUES
    ('DT-APQP-001', 'TMP-APQP-V1', 'PHT-APQP-001', 'PMT-APQP-001', 'PROJECT_CHARTER', 'Project Charter', 'Formal project charter and kickoff package.', 'DOCUMENT_PACKAGE', TRUE, 10, 'LEAD', 'CUSTOMER', 'ALL_PROJECT_PARTICIPANTS', 'HIGH'),
    ('DT-APQP-002', 'TMP-APQP-V1', 'PHT-APQP-002', 'PMT-APQP-002', 'DFMEA', 'DFMEA', 'Design FMEA package.', 'EVIDENCE_PACKAGE', TRUE, 45, 'SUPPLIER', 'LEAD', 'RESPONSIBLE_AND_APPROVER', 'HIGH'),
    ('DT-APQP-003', 'TMP-APQP-V1', 'PHT-APQP-003', 'PMT-APQP-003', 'PPAP_PACKAGE', 'PPAP Package', 'Production part approval package.', 'SUBMISSION_PACKAGE', TRUE, 88, 'SUPPLIER', 'CUSTOMER', 'RESPONSIBLE_AND_APPROVER', 'CRITICAL'),
    ('DT-VDA-001', 'TMP-VDA-MLA-V1', 'PHT-VDA-001', 'PMT-VDA-001', 'READINESS_ASSESSMENT', 'Readiness Assessment', 'Initial readiness baseline.', 'REPORT', TRUE, 18, 'LEAD', 'CUSTOMER', 'ALL_PROJECT_PARTICIPANTS', 'MEDIUM'),
    ('DT-VDA-002', 'TMP-VDA-MLA-V1', 'PHT-VDA-002', 'PMT-VDA-002', 'ACTION_PLAN', 'Action Plan', 'Action plan execution package.', 'CHECKLIST', TRUE, 60, 'SUPPLIER', 'LEAD', 'RESPONSIBLE_AND_APPROVER', 'HIGH'),
    ('DT-VDA-003', 'TMP-VDA-MLA-V1', 'PHT-VDA-003', 'PMT-VDA-003', 'MLA_EVIDENCE_PACKAGE', 'MLA Evidence Package', 'Final VDA MLA evidence package.', 'SUBMISSION_PACKAGE', TRUE, 92, 'SUPPLIER', 'CUSTOMER', 'RESPONSIBLE_AND_APPROVER', 'CRITICAL'),
    ('DT-CUS-001', 'TMP-CUSTOM-V1', 'PHT-CUS-001', 'PMT-CUS-001', 'CUSTOM_DELIVERABLE_1', 'Custom Deliverable', 'Generic template deliverable.', 'DOCUMENT_PACKAGE', FALSE, 35, 'LEAD', 'CUSTOMER', 'ALL_PROJECT_PARTICIPANTS', 'MEDIUM');
