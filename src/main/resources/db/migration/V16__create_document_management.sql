CREATE TABLE document (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    safe_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(160) NOT NULL,
    extension VARCHAR(16) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    storage_provider VARCHAR(32) NOT NULL,
    storage_key VARCHAR(512) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    uploaded_by_user_id VARCHAR(64) NOT NULL,
    uploaded_by_organization_id VARCHAR(64) NOT NULL,
    upload_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE document_binding (
    id VARCHAR(64) PRIMARY KEY,
    document_id VARCHAR(64) NOT NULL UNIQUE REFERENCES document(id),
    context_type VARCHAR(64) NOT NULL,
    context_id VARCHAR(128) NOT NULL,
    owner_organization_id VARCHAR(64) NOT NULL,
    created_by_user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_document_tenant_id ON document (tenant_id);
CREATE INDEX idx_document_status ON document (status);
CREATE INDEX idx_document_created_at ON document (created_at);
CREATE INDEX idx_document_uploaded_by_user_id ON document (uploaded_by_user_id);
CREATE INDEX idx_document_uploaded_by_organization_id ON document (uploaded_by_organization_id);
CREATE INDEX idx_document_upload_expires_at ON document (upload_expires_at);

CREATE INDEX idx_document_binding_context ON document_binding (context_type, context_id);
CREATE INDEX idx_document_binding_owner_organization_id ON document_binding (owner_organization_id);
CREATE INDEX idx_document_binding_created_at ON document_binding (created_at);
