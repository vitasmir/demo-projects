CREATE TABLE tm_projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(3000) NOT NULL
);

INSERT INTO tm_projects (name, description)
VALUES ('Default projekt', 'Migrated data from existing sprint setup');

ALTER TABLE tm_sprints ADD COLUMN project_id BIGINT;

UPDATE tm_sprints
SET project_id = (SELECT id FROM tm_projects ORDER BY id ASC LIMIT 1)
WHERE project_id IS NULL;

ALTER TABLE tm_sprints
    ALTER COLUMN project_id SET NOT NULL,
    ADD CONSTRAINT fk_tm_sprints_project FOREIGN KEY (project_id) REFERENCES tm_projects(id) ON DELETE CASCADE;

ALTER TABLE tm_user_stories ADD COLUMN project_id BIGINT;

UPDATE tm_user_stories us
SET project_id = s.project_id
FROM tm_sprints s
WHERE us.sprint_id = s.id
  AND us.project_id IS NULL;

UPDATE tm_user_stories
SET project_id = (SELECT id FROM tm_projects ORDER BY id ASC LIMIT 1)
WHERE project_id IS NULL;

ALTER TABLE tm_user_stories
    ALTER COLUMN project_id SET NOT NULL,
    ADD CONSTRAINT fk_tm_user_stories_project FOREIGN KEY (project_id) REFERENCES tm_projects(id) ON DELETE CASCADE;

ALTER TABLE tm_user_stories ALTER COLUMN sprint_id DROP NOT NULL;

CREATE INDEX idx_tm_sprint_project_id ON tm_sprints(project_id);
CREATE INDEX idx_tm_story_project_id ON tm_user_stories(project_id);
CREATE INDEX idx_tm_story_project_sprint ON tm_user_stories(project_id, sprint_id);
