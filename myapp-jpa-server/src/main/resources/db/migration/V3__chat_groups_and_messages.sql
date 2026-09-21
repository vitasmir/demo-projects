CREATE TABLE chat_groups (
    id VARCHAR(255) NOT NULL,
    name VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chat_groups_pkey PRIMARY KEY (id)
);

CREATE TABLE chat_messages (
    id VARCHAR(255) NOT NULL,
    group_id VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    color VARCHAR(16) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chat_messages_pkey PRIMARY KEY (id),
    CONSTRAINT chat_messages_group_fk FOREIGN KEY (group_id)
        REFERENCES chat_groups(id)
        ON DELETE CASCADE
);

CREATE INDEX chat_messages_group_sent_idx
    ON chat_messages (group_id, sent_at DESC);
