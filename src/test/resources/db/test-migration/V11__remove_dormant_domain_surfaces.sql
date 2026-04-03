-- Permanently remove dormant runtime surfaces outside the active User + Organization core.

DELETE FROM role_permission
WHERE permission_code LIKE 'portfolio.%'
   OR permission_code LIKE 'operations.%'
   OR permission_code LIKE 'reports.%';

DELETE FROM app_permission
WHERE code LIKE 'portfolio.%'
   OR code LIKE 'operations.%'
   OR code LIKE 'reports.%';

DROP TABLE IF EXISTS deliverable_document;
DROP TABLE IF EXISTS deliverable;
DROP TABLE IF EXISTS item_record;
DROP TABLE IF EXISTS product_record;
DROP TABLE IF EXISTS project_milestone;
DROP TABLE IF EXISTS open_issue;
DROP TABLE IF EXISTS project_record;
DROP TABLE IF EXISTS program_participation;
DROP TABLE IF EXISTS program_record;
DROP TABLE IF EXISTS milestone_template_item;
DROP TABLE IF EXISTS milestone_template;
DROP TABLE IF EXISTS operation_record;
