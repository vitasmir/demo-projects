ALTER TABLE tm_user_stories ADD COLUMN story_number INTEGER;

UPDATE tm_user_stories us
SET story_number = numbered.story_number
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY project_id ORDER BY id) AS story_number
    FROM tm_user_stories
) numbered
WHERE us.id = numbered.id;

ALTER TABLE tm_user_stories ALTER COLUMN story_number SET NOT NULL;

ALTER TABLE tm_tasks DROP CONSTRAINT IF EXISTS tm_tasks_task_key_key;

ALTER TABLE tm_tasks ADD COLUMN task_number INTEGER;

UPDATE tm_tasks t
SET task_number = numbered.task_number,
    task_key = 'task#' || numbered.task_number
FROM (
    SELECT t.id, ROW_NUMBER() OVER (PARTITION BY us.project_id ORDER BY t.id) AS task_number
    FROM tm_tasks t
    JOIN tm_user_stories us ON us.id = t.story_id
) numbered
WHERE t.id = numbered.id;

ALTER TABLE tm_tasks ALTER COLUMN task_number SET NOT NULL;

CREATE UNIQUE INDEX uq_tm_user_story_project_number ON tm_user_stories(project_id, story_number);
CREATE INDEX idx_tm_task_task_number ON tm_tasks(task_number);