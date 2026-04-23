UPDATE project_structure_template
SET owner_organization_id = 'internal-core'
WHERE id IN ('PST-APQP-V1', 'PST-VDA-MLA-V1', 'PST-CUSTOM-V1');

UPDATE project_template
SET owner_organization_id = 'internal-core'
WHERE id IN ('TMP-APQP-V1', 'TMP-VDA-MLA-V1', 'TMP-CUSTOM-V1');
