ALTER TABLE tenant
    ADD COLUMN service_tier VARCHAR(32) DEFAULT 'STANDARD';

UPDATE tenant
SET service_tier = CASE
    WHEN tenant_type = 'INTERNAL' THEN 'INTERNAL'
    ELSE 'STANDARD'
END
WHERE service_tier IS NULL;

ALTER TABLE tenant
    ALTER COLUMN service_tier SET NOT NULL;

ALTER TABLE organization
    ADD COLUMN lifecycle_state VARCHAR(32) DEFAULT 'ACTIVE';

ALTER TABLE organization
    ADD COLUMN offboarding_started_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE organization
    ADD COLUMN offboarded_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE organization
    ADD COLUMN retention_until TIMESTAMP WITH TIME ZONE;

ALTER TABLE organization
    ADD COLUMN data_export_status VARCHAR(32) DEFAULT 'NOT_REQUESTED';

ALTER TABLE organization
    ADD COLUMN data_exported_at TIMESTAMP WITH TIME ZONE;

UPDATE organization
SET lifecycle_state = CASE
    WHEN status = 'INACTIVE' THEN 'OFFBOARDED'
    ELSE 'ACTIVE'
END,
    data_export_status = CASE
        WHEN tenant_type = 'INTERNAL' THEN 'NOT_REQUIRED'
        WHEN status = 'INACTIVE' THEN 'READY_FOR_EXPORT'
        ELSE 'NOT_REQUESTED'
    END,
    offboarded_at = CASE
        WHEN status = 'INACTIVE' THEN updated_at
        ELSE offboarded_at
    END
WHERE lifecycle_state IS NULL
   OR data_export_status IS NULL;

ALTER TABLE organization
    ALTER COLUMN lifecycle_state SET NOT NULL;

ALTER TABLE organization
    ALTER COLUMN data_export_status SET NOT NULL;

ALTER TABLE user_membership
    ADD COLUMN lifecycle_state VARCHAR(32) DEFAULT 'ACTIVE';

ALTER TABLE user_membership
    ADD COLUMN access_revoked_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE user_membership
    ADD COLUMN offboarded_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE user_membership
    ADD COLUMN retention_until TIMESTAMP WITH TIME ZONE;

UPDATE user_membership
SET lifecycle_state = CASE
    WHEN status = 'INACTIVE' THEN 'REVOKED'
    ELSE 'ACTIVE'
END,
    access_revoked_at = CASE
        WHEN status = 'INACTIVE' THEN updated_at
        ELSE access_revoked_at
    END
WHERE lifecycle_state IS NULL;

ALTER TABLE user_membership
    ALTER COLUMN lifecycle_state SET NOT NULL;
