CREATE TABLE app_user (
    id VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(32) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    tenant_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    invite_resent_at TIMESTAMP WITH TIME ZONE,
    access_reset_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_app_user_tenant_id ON app_user (tenant_id);
CREATE INDEX idx_app_user_role ON app_user (role);

CREATE TABLE operation_record (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    tenant_id VARCHAR(64) NOT NULL,
    tenant_type VARCHAR(32) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    reopened_at TIMESTAMP WITH TIME ZONE,
    reprocessed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_operation_record_tenant_id ON operation_record (tenant_id);
CREATE INDEX idx_operation_record_status ON operation_record (status);
