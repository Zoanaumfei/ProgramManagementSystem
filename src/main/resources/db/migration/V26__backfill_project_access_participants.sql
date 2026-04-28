INSERT INTO project_organization (id, project_id, organization_id, role_type, joined_at, active)
SELECT
    'POR-' || SUBSTRING(p.id, 1, 54) || '-LEAD',
    p.id,
    p.lead_organization_id,
    'LEAD',
    p.created_at,
    TRUE
FROM project p
WHERE p.lead_organization_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM project_organization po
      WHERE po.project_id = p.id
        AND po.organization_id = p.lead_organization_id
  );

INSERT INTO project_organization (id, project_id, organization_id, role_type, joined_at, active)
SELECT
    'POR-' || SUBSTRING(p.id, 1, 54) || '-CUS',
    p.id,
    p.customer_organization_id,
    'CUSTOMER',
    p.created_at,
    TRUE
FROM project p
WHERE p.customer_organization_id IS NOT NULL
  AND p.customer_organization_id <> p.lead_organization_id
  AND NOT EXISTS (
      SELECT 1
      FROM project_organization po
      WHERE po.project_id = p.id
        AND po.organization_id = p.customer_organization_id
  );

INSERT INTO project_member (id, project_id, user_id, organization_id, project_role, active, assigned_at)
SELECT
    'PMB-' || SUBSTRING(p.id, 1, 54) || '-OWN',
    p.id,
    p.created_by_user_id,
    p.lead_organization_id,
    'PROJECT_MANAGER',
    TRUE,
    p.created_at
FROM project p
WHERE p.created_by_user_id IS NOT NULL
  AND p.lead_organization_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM project_member pm
      WHERE pm.project_id = p.id
        AND pm.user_id = p.created_by_user_id
  );
