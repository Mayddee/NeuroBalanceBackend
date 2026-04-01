DROP TABLE IF EXISTS note_users_notes CASCADE;
DROP TABLE IF EXISTS note_users CASCADE;
DROP TABLE IF EXISTS notes CASCADE; -- Добавьте эту строку

CREATE TABLE note_users (
                            user_id BIGINT PRIMARY KEY,
                            username VARCHAR(255) UNIQUE,
                            email VARCHAR(255),
                            name VARCHAR(255)
);

CREATE TABLE notes (
                       id BIGINT PRIMARY KEY,
                       title VARCHAR(255),
                       content TEXT,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE note_users_notes (
                                  user_id BIGINT NOT NULL,
                                  note_id BIGINT NOT NULL,
                                  PRIMARY KEY (user_id, note_id),
                                  FOREIGN KEY (user_id) REFERENCES note_users(user_id) ON DELETE CASCADE,
                                  FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE -- Теперь таблица notes существует!
);

-- 5. Индексы
CREATE INDEX idx_note_users_username ON note_users(username);
CREATE INDEX idx_note_users_notes_user_id ON note_users_notes(user_id);
CREATE INDEX idx_note_users_notes_note_id ON note_users_notes(note_id);