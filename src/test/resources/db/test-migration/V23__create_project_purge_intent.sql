CREATE TABLE project_purge_intent (
    token VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    requested_by_user_id VARCHAR(64),
    requested_by_username VARCHAR(160),
    reason TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_project_purge_intent_project_id ON project_purge_intent (project_id);
CREATE INDEX idx_project_purge_intent_status_expires_at ON project_purge_intent (status, expires_at);
