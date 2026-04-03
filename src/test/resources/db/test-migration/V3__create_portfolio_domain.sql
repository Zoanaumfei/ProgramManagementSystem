CREATE TABLE organization (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(80) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE TABLE milestone_template (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE TABLE milestone_template_item (
    id VARCHAR(64) PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL REFERENCES milestone_template(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    sort_order INTEGER NOT NULL,
    required_flag BOOLEAN NOT NULL,
    offset_weeks INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE TABLE program_record (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(80) NOT NULL UNIQUE,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    planned_start_date DATE NOT NULL,
    planned_end_date DATE NOT NULL,
    owner_organization_id VARCHAR(64) NOT NULL REFERENCES organization(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE TABLE program_participation (
    id VARCHAR(64) PRIMARY KEY,
    program_id VARCHAR(64) NOT NULL REFERENCES program_record(id) ON DELETE CASCADE,
    organization_id VARCHAR(64) NOT NULL REFERENCES organization(id),
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE TABLE project_record (
    id VARCHAR(64) PRIMARY KEY,
    program_id VARCHAR(64) NOT NULL REFERENCES program_record(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(80) NOT NULL UNIQUE,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    planned_start_date DATE NOT NULL,
    planned_end_date DATE NOT NULL,
    applied_milestone_template_id VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE TABLE product_record (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES project_record(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(80) NOT NULL UNIQUE,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE TABLE item_record (
    id VARCHAR(64) PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL REFERENCES product_record(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(80) NOT NULL UNIQUE,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE TABLE deliverable (
    id VARCHAR(64) PRIMARY KEY,
    item_id VARCHAR(64) NOT NULL REFERENCES item_record(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    planned_date DATE NOT NULL,
    due_date DATE NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE TABLE project_milestone (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES project_record(id) ON DELETE CASCADE,
    milestone_template_item_id VARCHAR(64),
    name VARCHAR(160) NOT NULL,
    sort_order INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    planned_date DATE NOT NULL,
    actual_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE TABLE open_issue (
    id VARCHAR(64) PRIMARY KEY,
    program_id VARCHAR(64) NOT NULL REFERENCES program_record(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE INDEX idx_program_owner_org ON program_record (owner_organization_id);
CREATE INDEX idx_program_participation_program ON program_participation (program_id);
CREATE INDEX idx_program_participation_org ON program_participation (organization_id);
CREATE INDEX idx_project_program ON project_record (program_id);
CREATE INDEX idx_product_project ON product_record (project_id);
CREATE INDEX idx_item_product ON item_record (product_id);
CREATE INDEX idx_deliverable_item ON deliverable (item_id);
CREATE INDEX idx_project_milestone_project ON project_milestone (project_id);
CREATE INDEX idx_open_issue_program ON open_issue (program_id);
