ALTER TABLE app_user
    ADD COLUMN identity_username VARCHAR(255);

ALTER TABLE app_user
    ADD COLUMN identity_subject VARCHAR(255);

UPDATE app_user
SET identity_username = LOWER(email)
WHERE identity_username IS NULL;

ALTER TABLE app_user
    ALTER COLUMN identity_username SET NOT NULL;

CREATE UNIQUE INDEX idx_app_user_identity_username ON app_user (identity_username);
CREATE UNIQUE INDEX idx_app_user_identity_subject ON app_user (identity_subject);
