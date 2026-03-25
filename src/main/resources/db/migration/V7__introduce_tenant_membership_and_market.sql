ALTER TABLE app_user
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

UPDATE app_user
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE app_user
    ALTER COLUMN updated_at SET NOT NULL;

CREATE TABLE tenant (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(80) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    tenant_type VARCHAR(32) NOT NULL,
    data_region VARCHAR(64),
    root_organization_id VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE tenant_market (
    id VARCHAR(96) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency_code VARCHAR(16),
    language_code VARCHAR(16),
    timezone VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_tenant_market_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE UNIQUE INDEX uq_tenant_market_tenant_code ON tenant_market (tenant_id, code);

CREATE TABLE app_role (
    code VARCHAR(64) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL
);

CREATE TABLE app_permission (
    code VARCHAR(96) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE role_permission (
    id VARCHAR(160) PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL,
    permission_code VARCHAR(96) NOT NULL,
    CONSTRAINT fk_role_permission_role
        FOREIGN KEY (role_code) REFERENCES app_role(code),
    CONSTRAINT fk_role_permission_permission
        FOREIGN KEY (permission_code) REFERENCES app_permission(code)
);

CREATE UNIQUE INDEX uq_role_permission_role_code_permission_code
    ON role_permission (role_code, permission_code);

CREATE TABLE user_membership (
    id VARCHAR(96) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    organization_id VARCHAR(64),
    market_id VARCHAR(96),
    status VARCHAR(32) NOT NULL,
    is_default BOOLEAN NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_user_membership_user
        FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_user_membership_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_user_membership_organization
        FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_user_membership_market
        FOREIGN KEY (market_id) REFERENCES tenant_market(id)
);

CREATE INDEX idx_user_membership_user_id ON user_membership (user_id);
CREATE INDEX idx_user_membership_tenant_id ON user_membership (tenant_id);
CREATE INDEX idx_user_membership_organization_id ON user_membership (organization_id);
CREATE UNIQUE INDEX uq_user_membership_default_per_user ON user_membership (user_id, is_default);

CREATE TABLE membership_role (
    id VARCHAR(160) PRIMARY KEY,
    membership_id VARCHAR(96) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    CONSTRAINT fk_membership_role_membership
        FOREIGN KEY (membership_id) REFERENCES user_membership(id),
    CONSTRAINT fk_membership_role_role
        FOREIGN KEY (role_code) REFERENCES app_role(code)
);

CREATE UNIQUE INDEX uq_membership_role_membership_code ON membership_role (membership_id, role_code);

ALTER TABLE organization
    ADD COLUMN tenant_id VARCHAR(64);

ALTER TABLE organization
    ADD COLUMN market_id VARCHAR(96);

INSERT INTO tenant (id, name, code, status, tenant_type, data_region, root_organization_id, created_at, updated_at)
SELECT
    'TEN-' || id,
    name,
    code,
    CASE WHEN status = 'ACTIVE' THEN 'ACTIVE' ELSE 'INACTIVE' END,
    tenant_type,
    'sa-east-1',
    id,
    created_at,
    updated_at
FROM organization
WHERE parent_organization_id IS NULL;

INSERT INTO tenant_market (id, tenant_id, code, name, status, currency_code, language_code, timezone, created_at, updated_at)
SELECT
    'MKT-DEFAULT-' || id,
    id,
    'DEFAULT',
    'Default Market',
    'ACTIVE',
    NULL,
    NULL,
    'UTC',
    created_at,
    updated_at
FROM tenant;

UPDATE organization
SET tenant_id = CASE
    WHEN customer_organization_id IS NOT NULL THEN 'TEN-' || customer_organization_id
    ELSE 'TEN-' || id
END
WHERE tenant_id IS NULL;

ALTER TABLE organization
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE organization
    ADD CONSTRAINT fk_organization_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant(id);

ALTER TABLE organization
    ADD CONSTRAINT fk_organization_market
        FOREIGN KEY (market_id) REFERENCES tenant_market(id);

CREATE INDEX idx_organization_tenant_id ON organization (tenant_id);
CREATE INDEX idx_organization_market_id ON organization (market_id);

INSERT INTO app_role (code, name, description, active) VALUES
    ('ADMIN', 'Admin', 'Legacy administrative role kept during the membership-first transition.', TRUE),
    ('MANAGER', 'Manager', 'Legacy manager role kept during the membership-first transition.', TRUE),
    ('MEMBER', 'Member', 'Legacy member role kept during the membership-first transition.', TRUE),
    ('SUPPORT', 'Support', 'Legacy support role kept during the membership-first transition.', TRUE),
    ('AUDITOR', 'Auditor', 'Legacy auditor role kept during the membership-first transition.', TRUE);

INSERT INTO app_permission (code, name, description) VALUES
    ('users.view', 'Users View', 'View user directory and user details.'),
    ('users.create', 'Users Create', 'Create new user accounts.'),
    ('users.edit', 'Users Edit', 'Edit existing user accounts.'),
    ('users.delete', 'Users Delete', 'Inactivate or delete user accounts.'),
    ('users.reset_access', 'Users Reset Access', 'Reset user access and credentials.'),
    ('users.resend_invite', 'Users Resend Invite', 'Resend user invitations.'),
    ('users.assign_role', 'Users Assign Role', 'Assign roles inside a membership context.'),
    ('tenant.view', 'Tenant View', 'View tenant and organization administration surfaces.'),
    ('tenant.create', 'Tenant Create', 'Create tenant or top-level organization structures.'),
    ('tenant.edit', 'Tenant Edit', 'Edit tenant or organization structures.'),
    ('tenant.delete', 'Tenant Delete', 'Inactivate tenant or organization structures.'),
    ('tenant.configure', 'Tenant Configure', 'Configure tenant integrations and settings.'),
    ('tenant.manage_integration', 'Tenant Manage Integration', 'Manage tenant integrations.'),
    ('tenant.purge', 'Tenant Purge', 'Purge tenant-owned structures under operational override.'),
    ('portfolio.view', 'Portfolio View', 'View portfolio data.'),
    ('portfolio.create', 'Portfolio Create', 'Create portfolio structures.'),
    ('portfolio.edit', 'Portfolio Edit', 'Edit portfolio structures.'),
    ('portfolio.delete', 'Portfolio Delete', 'Delete portfolio structures.'),
    ('portfolio.configure', 'Portfolio Configure', 'Configure portfolio templates and governance.'),
    ('operations.view', 'Operations View', 'View operations.'),
    ('operations.create', 'Operations Create', 'Create operations.'),
    ('operations.edit', 'Operations Edit', 'Edit operations.'),
    ('operations.delete', 'Operations Delete', 'Delete operations.'),
    ('operations.approve', 'Operations Approve', 'Approve operations.'),
    ('operations.reject', 'Operations Reject', 'Reject operations.'),
    ('operations.submit', 'Operations Submit', 'Submit operations.'),
    ('operations.reopen', 'Operations Reopen', 'Reopen operations.'),
    ('operations.reprocess', 'Operations Reprocess', 'Reprocess operations.'),
    ('reports.view', 'Reports View', 'View reports.'),
    ('reports.export', 'Reports Export', 'Export reports.'),
    ('audit.view', 'Audit View', 'View audit trail entries.'),
    ('audit.export', 'Audit Export', 'Export audit trail entries.'),
    ('audit.view_security_events', 'Audit Security Events', 'View security events.'),
    ('support.view', 'Support View', 'Access support read surfaces.'),
    ('support.reprocess', 'Support Reprocess', 'Reprocess support workflows.'),
    ('support.impersonate', 'Support Impersonate', 'Impersonate another context under override.'),
    ('support.open_intervention', 'Support Intervention', 'Open support interventions.'),
    ('platform.view', 'Platform View', 'View platform administration surfaces.'),
    ('platform.configure', 'Platform Configure', 'Configure platform settings.'),
    ('platform.impersonate', 'Platform Impersonate', 'Impersonate at platform level.');

INSERT INTO role_permission (id, role_code, permission_code) VALUES
    ('RP-ADMIN-users.view', 'ADMIN', 'users.view'),
    ('RP-ADMIN-users.create', 'ADMIN', 'users.create'),
    ('RP-ADMIN-users.edit', 'ADMIN', 'users.edit'),
    ('RP-ADMIN-users.delete', 'ADMIN', 'users.delete'),
    ('RP-ADMIN-users.reset_access', 'ADMIN', 'users.reset_access'),
    ('RP-ADMIN-users.resend_invite', 'ADMIN', 'users.resend_invite'),
    ('RP-ADMIN-users.assign_role', 'ADMIN', 'users.assign_role'),
    ('RP-ADMIN-tenant.view', 'ADMIN', 'tenant.view'),
    ('RP-ADMIN-tenant.create', 'ADMIN', 'tenant.create'),
    ('RP-ADMIN-tenant.edit', 'ADMIN', 'tenant.edit'),
    ('RP-ADMIN-tenant.delete', 'ADMIN', 'tenant.delete'),
    ('RP-ADMIN-tenant.configure', 'ADMIN', 'tenant.configure'),
    ('RP-ADMIN-tenant.manage_integration', 'ADMIN', 'tenant.manage_integration'),
    ('RP-ADMIN-portfolio.view', 'ADMIN', 'portfolio.view'),
    ('RP-ADMIN-portfolio.create', 'ADMIN', 'portfolio.create'),
    ('RP-ADMIN-portfolio.edit', 'ADMIN', 'portfolio.edit'),
    ('RP-ADMIN-portfolio.delete', 'ADMIN', 'portfolio.delete'),
    ('RP-ADMIN-portfolio.configure', 'ADMIN', 'portfolio.configure'),
    ('RP-ADMIN-operations.view', 'ADMIN', 'operations.view'),
    ('RP-ADMIN-operations.create', 'ADMIN', 'operations.create'),
    ('RP-ADMIN-operations.edit', 'ADMIN', 'operations.edit'),
    ('RP-ADMIN-operations.delete', 'ADMIN', 'operations.delete'),
    ('RP-ADMIN-operations.approve', 'ADMIN', 'operations.approve'),
    ('RP-ADMIN-operations.reject', 'ADMIN', 'operations.reject'),
    ('RP-ADMIN-operations.submit', 'ADMIN', 'operations.submit'),
    ('RP-ADMIN-operations.reopen', 'ADMIN', 'operations.reopen'),
    ('RP-ADMIN-operations.reprocess', 'ADMIN', 'operations.reprocess'),
    ('RP-ADMIN-reports.view', 'ADMIN', 'reports.view'),
    ('RP-ADMIN-reports.export', 'ADMIN', 'reports.export'),
    ('RP-ADMIN-audit.view', 'ADMIN', 'audit.view'),
    ('RP-ADMIN-audit.export', 'ADMIN', 'audit.export'),
    ('RP-ADMIN-audit.view_security_events', 'ADMIN', 'audit.view_security_events'),
    ('RP-ADMIN-support.view', 'ADMIN', 'support.view'),
    ('RP-ADMIN-support.reprocess', 'ADMIN', 'support.reprocess'),
    ('RP-ADMIN-support.impersonate', 'ADMIN', 'support.impersonate'),
    ('RP-ADMIN-support.open_intervention', 'ADMIN', 'support.open_intervention'),
    ('RP-ADMIN-platform.view', 'ADMIN', 'platform.view'),
    ('RP-ADMIN-platform.configure', 'ADMIN', 'platform.configure'),
    ('RP-ADMIN-platform.impersonate', 'ADMIN', 'platform.impersonate'),
    ('RP-MANAGER-portfolio.view', 'MANAGER', 'portfolio.view'),
    ('RP-MANAGER-portfolio.create', 'MANAGER', 'portfolio.create'),
    ('RP-MANAGER-portfolio.edit', 'MANAGER', 'portfolio.edit'),
    ('RP-MANAGER-portfolio.delete', 'MANAGER', 'portfolio.delete'),
    ('RP-MANAGER-operations.view', 'MANAGER', 'operations.view'),
    ('RP-MANAGER-operations.create', 'MANAGER', 'operations.create'),
    ('RP-MANAGER-operations.edit', 'MANAGER', 'operations.edit'),
    ('RP-MANAGER-operations.delete', 'MANAGER', 'operations.delete'),
    ('RP-MANAGER-operations.approve', 'MANAGER', 'operations.approve'),
    ('RP-MANAGER-operations.reject', 'MANAGER', 'operations.reject'),
    ('RP-MANAGER-operations.submit', 'MANAGER', 'operations.submit'),
    ('RP-MANAGER-operations.reopen', 'MANAGER', 'operations.reopen'),
    ('RP-MANAGER-operations.reprocess', 'MANAGER', 'operations.reprocess'),
    ('RP-MANAGER-reports.view', 'MANAGER', 'reports.view'),
    ('RP-MANAGER-reports.export', 'MANAGER', 'reports.export'),
    ('RP-MANAGER-audit.view', 'MANAGER', 'audit.view'),
    ('RP-MANAGER-audit.export', 'MANAGER', 'audit.export'),
    ('RP-MANAGER-audit.view_security_events', 'MANAGER', 'audit.view_security_events'),
    ('RP-MANAGER-support.view', 'MANAGER', 'support.view'),
    ('RP-MANAGER-support.reprocess', 'MANAGER', 'support.reprocess'),
    ('RP-MANAGER-support.open_intervention', 'MANAGER', 'support.open_intervention'),
    ('RP-MEMBER-portfolio.view', 'MEMBER', 'portfolio.view'),
    ('RP-MEMBER-portfolio.create', 'MEMBER', 'portfolio.create'),
    ('RP-MEMBER-portfolio.edit', 'MEMBER', 'portfolio.edit'),
    ('RP-MEMBER-portfolio.delete', 'MEMBER', 'portfolio.delete'),
    ('RP-MEMBER-operations.view', 'MEMBER', 'operations.view'),
    ('RP-MEMBER-operations.create', 'MEMBER', 'operations.create'),
    ('RP-MEMBER-operations.edit', 'MEMBER', 'operations.edit'),
    ('RP-MEMBER-operations.submit', 'MEMBER', 'operations.submit'),
    ('RP-MEMBER-reports.view', 'MEMBER', 'reports.view'),
    ('RP-MEMBER-reports.export', 'MEMBER', 'reports.export'),
    ('RP-SUPPORT-users.view', 'SUPPORT', 'users.view'),
    ('RP-SUPPORT-users.reset_access', 'SUPPORT', 'users.reset_access'),
    ('RP-SUPPORT-users.resend_invite', 'SUPPORT', 'users.resend_invite'),
    ('RP-SUPPORT-tenant.view', 'SUPPORT', 'tenant.view'),
    ('RP-SUPPORT-tenant.purge', 'SUPPORT', 'tenant.purge'),
    ('RP-SUPPORT-portfolio.view', 'SUPPORT', 'portfolio.view'),
    ('RP-SUPPORT-operations.view', 'SUPPORT', 'operations.view'),
    ('RP-SUPPORT-operations.reprocess', 'SUPPORT', 'operations.reprocess'),
    ('RP-SUPPORT-reports.view', 'SUPPORT', 'reports.view'),
    ('RP-SUPPORT-reports.export', 'SUPPORT', 'reports.export'),
    ('RP-SUPPORT-audit.view', 'SUPPORT', 'audit.view'),
    ('RP-SUPPORT-audit.export', 'SUPPORT', 'audit.export'),
    ('RP-SUPPORT-audit.view_security_events', 'SUPPORT', 'audit.view_security_events'),
    ('RP-SUPPORT-support.view', 'SUPPORT', 'support.view'),
    ('RP-SUPPORT-support.reprocess', 'SUPPORT', 'support.reprocess'),
    ('RP-SUPPORT-support.impersonate', 'SUPPORT', 'support.impersonate'),
    ('RP-SUPPORT-support.open_intervention', 'SUPPORT', 'support.open_intervention'),
    ('RP-SUPPORT-platform.view', 'SUPPORT', 'platform.view'),
    ('RP-AUDITOR-portfolio.view', 'AUDITOR', 'portfolio.view'),
    ('RP-AUDITOR-operations.view', 'AUDITOR', 'operations.view'),
    ('RP-AUDITOR-reports.view', 'AUDITOR', 'reports.view'),
    ('RP-AUDITOR-reports.export', 'AUDITOR', 'reports.export'),
    ('RP-AUDITOR-audit.view', 'AUDITOR', 'audit.view'),
    ('RP-AUDITOR-audit.export', 'AUDITOR', 'audit.export'),
    ('RP-AUDITOR-audit.view_security_events', 'AUDITOR', 'audit.view_security_events'),
    ('RP-AUDITOR-platform.view', 'AUDITOR', 'platform.view');

INSERT INTO user_membership (id, user_id, tenant_id, organization_id, market_id, status, is_default, joined_at, updated_at)
SELECT
    'MBR-' || u.id,
    u.id,
    COALESCE(o.tenant_id, 'TEN-' || u.tenant_id),
    u.tenant_id,
    NULL,
    CASE WHEN u.status = 'INACTIVE' THEN 'INACTIVE' ELSE 'ACTIVE' END,
    TRUE,
    u.created_at,
    u.updated_at
FROM app_user u
LEFT JOIN organization o ON o.id = u.tenant_id;

INSERT INTO membership_role (id, membership_id, role_code)
SELECT
    'MBRROLE-' || u.id || '-' || u.role,
    'MBR-' || u.id,
    u.role
FROM app_user u;
