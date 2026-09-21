CREATE TABLE tm_task_discussion_messages (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tm_tasks(id) ON DELETE CASCADE,
    author_id VARCHAR(255) NOT NULL REFERENCES tm_users(id) ON DELETE CASCADE,
    parent_message_id BIGINT REFERENCES tm_task_discussion_messages(id) ON DELETE CASCADE,
    content VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE tm_task_discussion_reads (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tm_tasks(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL REFERENCES tm_users(id) ON DELETE CASCADE,
    last_read_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_tm_task_discussion_task_user UNIQUE (task_id, user_id)
);

CREATE INDEX idx_tm_task_discussion_task_created
    ON tm_task_discussion_messages (task_id, created_at, id);

CREATE INDEX idx_tm_task_discussion_project_author
    ON tm_task_discussion_messages (author_id, created_at DESC);

CREATE INDEX idx_tm_task_discussion_reads_user_task
    ON tm_task_discussion_reads (user_id, task_id);
