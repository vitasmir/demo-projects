DROP INDEX IF EXISTS uq_tm_task_story_number;
DROP INDEX IF EXISTS idx_tm_task_task_number;

UPDATE tm_tasks t
SET task_number = numbered.task_number,
    task_key = 'task#' || numbered.task_number
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY story_id ORDER BY id) AS task_number
    FROM tm_tasks
) numbered
WHERE t.id = numbered.id;

CREATE UNIQUE INDEX uq_tm_task_story_number ON tm_tasks(story_id, task_number);