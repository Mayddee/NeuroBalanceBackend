-- V1__init_users_and_notes.sql

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(255),
                       email VARCHAR(255),
                       username VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE notes (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(255),
                       content TEXT,
                       created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users_notes (
                             user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             note_id BIGINT NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
                             PRIMARY KEY (user_id, note_id)
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_notes_user_id ON users_notes(user_id);
CREATE INDEX idx_users_notes_note_id ON users_notes(note_id);