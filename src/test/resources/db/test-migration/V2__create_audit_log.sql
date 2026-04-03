CREATE TABLE audit_log (
    id VARCHAR(64) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    actor_user_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    actor_tenant_id VARCHAR(64),
    target_tenant_id VARCHAR(64),
    target_resource_type VARCHAR(64) NOT NULL,
    target_resource_id VARCHAR(64),
    justification TEXT,
    metadata_json TEXT,
    cross_tenant BOOLEAN NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    source_module VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
CREATE INDEX idx_audit_log_actor_user_id ON audit_log (actor_user_id);
CREATE INDEX idx_audit_log_target_tenant_id ON audit_log (target_tenant_id);
CREATE INDEX idx_audit_log_correlation_id ON audit_log (correlation_id);
CREATE INDEX idx_audit_log_source_module ON audit_log (source_module);
