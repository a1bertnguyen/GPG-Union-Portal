ALTER TABLE admin_users ADD COLUMN union_unit_id BIGINT NULL;
ALTER TABLE admin_users
    ADD CONSTRAINT fk_admin_user_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id);

CREATE INDEX idx_admin_users_unit ON admin_users(union_unit_id);
