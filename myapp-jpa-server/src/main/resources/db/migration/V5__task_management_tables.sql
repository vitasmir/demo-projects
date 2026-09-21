CREATE TABLE tm_users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE tm_sprints (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL
);

CREATE TABLE tm_user_stories (
    id BIGSERIAL PRIMARY KEY,
    sprint_id BIGINT NOT NULL REFERENCES tm_sprints(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(3000) NOT NULL,
    difficulty INT NOT NULL,
    CONSTRAINT chk_tm_story_difficulty CHECK (difficulty IN (5, 10, 20, 50, 100))
);

CREATE TABLE tm_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_key VARCHAR(255) NOT NULL UNIQUE,
    story_id BIGINT NOT NULL REFERENCES tm_user_stories(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(3000) NOT NULL,
    definition_of_done VARCHAR(3000) NOT NULL,
    color_hex VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL,
    creator_id VARCHAR(255) NOT NULL REFERENCES tm_users(id),
    assignee_id VARCHAR(255) NOT NULL REFERENCES tm_users(id),
    reviewer_id VARCHAR(255) NOT NULL REFERENCES tm_users(id),
    review_comment VARCHAR(2000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE TABLE tm_task_code_references (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tm_tasks(id) ON DELETE CASCADE,
    commit_hash VARCHAR(255) NOT NULL,
    repository_url VARCHAR(1000),
    note VARCHAR(1000),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_tm_story_sprint_id ON tm_user_stories(sprint_id);
CREATE INDEX idx_tm_task_story_id ON tm_tasks(story_id);
CREATE INDEX idx_tm_task_completed_at ON tm_tasks(completed_at);
CREATE INDEX idx_tm_code_ref_task_id ON tm_task_code_references(task_id);
