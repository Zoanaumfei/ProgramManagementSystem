DROP INDEX IF EXISTS idx_app_user_tenant_id;
DROP INDEX IF EXISTS idx_app_user_role;

ALTER TABLE app_user
    DROP COLUMN role;

ALTER TABLE app_user
    DROP COLUMN tenant_id;

ALTER TABLE app_user
    DROP COLUMN tenant_type;
