-- Reset portfolio domain data while preserving the access core.
-- The schema stays intact so portfolio can be resumed later.

DELETE FROM deliverable_document;
DELETE FROM deliverable;
DELETE FROM item_record;
DELETE FROM product_record;
DELETE FROM project_milestone;
DELETE FROM open_issue;
DELETE FROM project_record;
DELETE FROM program_participation;
DELETE FROM program_record;
DELETE FROM milestone_template_item;
DELETE FROM milestone_template;
