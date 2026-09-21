UPDATE tm_tasks
SET task_key = 'task#' || task_number
WHERE task_number IS NOT NULL;