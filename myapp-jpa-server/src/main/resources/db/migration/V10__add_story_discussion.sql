CREATE TABLE tm_story_discussion_messages (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL REFERENCES tm_user_stories(id) ON DELETE CASCADE,
    author_id VARCHAR(255) NOT NULL REFERENCES tm_users(id) ON DELETE CASCADE,
    parent_message_id BIGINT REFERENCES tm_story_discussion_messages(id) ON DELETE CASCADE,
    content VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE tm_story_discussion_reads (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL REFERENCES tm_user_stories(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL REFERENCES tm_users(id) ON DELETE CASCADE,
    last_read_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_tm_story_discussion_story_user UNIQUE (story_id, user_id)
);

CREATE INDEX idx_tm_story_discussion_story_created
    ON tm_story_discussion_messages (story_id, created_at, id);

CREATE INDEX idx_tm_story_discussion_project_author
    ON tm_story_discussion_messages (author_id, created_at DESC);

CREATE INDEX idx_tm_story_discussion_reads_user_story
    ON tm_story_discussion_reads (user_id, story_id);
