CREATE TABLE project_framework (
    id VARCHAR(64) PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    description TEXT,
    ui_layout VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_project_framework_code UNIQUE (code)
);

INSERT INTO project_framework (id, code, display_name, description, ui_layout, active, created_at, updated_at) VALUES
    ('PFR-APQP', 'APQP', 'APQP', 'Sequential product quality planning framework.', 'TIMELINE', TRUE, TIMESTAMP WITH TIME ZONE '2026-04-22 00:00:00+00', TIMESTAMP WITH TIME ZONE '2026-04-22 00:00:00+00'),
    ('PFR-VDA-MLA', 'VDA_MLA', 'VDA MLA', 'Sequential maturity level assurance framework.', 'TIMELINE', TRUE, TIMESTAMP WITH TIME ZONE '2026-04-22 00:00:00+00', TIMESTAMP WITH TIME ZONE '2026-04-22 00:00:00+00'),
    ('PFR-CUSTOM', 'CUSTOM', 'Custom', 'Flexible framework for tenant-defined project delivery flows.', 'HYBRID', TRUE, TIMESTAMP WITH TIME ZONE '2026-04-22 00:00:00+00', TIMESTAMP WITH TIME ZONE '2026-04-22 00:00:00+00');

ALTER TABLE project_template
    ADD CONSTRAINT fk_project_template_framework
        FOREIGN KEY (framework_type) REFERENCES project_framework(code);

ALTER TABLE project_structure_template
    ADD CONSTRAINT fk_project_structure_template_framework
        FOREIGN KEY (framework_type) REFERENCES project_framework(code);

ALTER TABLE project
    ADD CONSTRAINT fk_project_framework
        FOREIGN KEY (framework_type) REFERENCES project_framework(code);
