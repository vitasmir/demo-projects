ALTER TABLE tm_users
    ADD COLUMN admin BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE tm_project_members (
    project_id BIGINT NOT NULL REFERENCES tm_projects(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL REFERENCES tm_users(id) ON DELETE CASCADE,
    CONSTRAINT pk_tm_project_members PRIMARY KEY (project_id, user_id)
);

INSERT INTO tm_project_members (project_id, user_id)
SELECT p.id, u.id
FROM tm_projects p
CROSS JOIN tm_users u
ON CONFLICT DO NOTHING;

CREATE INDEX idx_tm_project_members_user_id ON tm_project_members(user_id);