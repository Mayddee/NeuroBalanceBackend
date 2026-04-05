-- ===============================================
-- NOTE-AI DATABASE SCHEMA
-- V1: Initial schema with note_users and notes
-- ===============================================

-- 1. Drop existing tables (if any)
DROP TABLE IF EXISTS note_users_notes CASCADE;
DROP TABLE IF EXISTS notes CASCADE;
DROP TABLE IF EXISTS note_users CASCADE;

-- 2. Create note_users table
CREATE TABLE note_users (
                            user_id BIGINT PRIMARY KEY,
                            username VARCHAR(255) UNIQUE,
                            email VARCHAR(255),
                            name VARCHAR(255)
);

-- 3. Create notes table with SERIAL ID
CREATE TABLE notes (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(500),
                       content TEXT,
                       created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Create junction table
CREATE TABLE note_users_notes (
                                  user_id BIGINT NOT NULL,
                                  note_id BIGINT NOT NULL,
                                  PRIMARY KEY (user_id, note_id),
                                  FOREIGN KEY (user_id) REFERENCES note_users(user_id) ON DELETE CASCADE,
                                  FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
);

-- 5. Create indexes
CREATE INDEX idx_note_users_username ON note_users(username);
CREATE INDEX idx_note_users_notes_user_id ON note_users_notes(user_id);
CREATE INDEX idx_note_users_notes_note_id ON note_users_notes(note_id);