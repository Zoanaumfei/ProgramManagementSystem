CREATE TABLE deliverable_document (
    id VARCHAR(64) PRIMARY KEY,
    deliverable_id VARCHAR(64) NOT NULL REFERENCES deliverable(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(160) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_bucket VARCHAR(255) NOT NULL,
    storage_key VARCHAR(1024) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE INDEX idx_deliverable_document_deliverable ON deliverable_document (deliverable_id);
CREATE INDEX idx_deliverable_document_status ON deliverable_document (status);
