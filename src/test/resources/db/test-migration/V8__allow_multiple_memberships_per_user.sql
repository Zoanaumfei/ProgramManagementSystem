DROP INDEX IF EXISTS uq_user_membership_default_per_user;

CREATE INDEX idx_user_membership_user_default
    ON user_membership (user_id, is_default);
