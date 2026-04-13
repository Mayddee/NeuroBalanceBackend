-- Таблица результатов когнитивных тестов
CREATE TABLE IF NOT EXISTS brain_game_results (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  user_id BIGINT NOT NULL,
                                                  game_type VARCHAR(50) NOT NULL, -- MEMORY_PAIRS, NUMBER_SEQUENCE

    score INTEGER NOT NULL DEFAULT 0,
    time_taken_seconds INTEGER NOT NULL,
    xp_earned INTEGER NOT NULL DEFAULT 0,
    is_win BOOLEAN NOT NULL DEFAULT FALSE,

    difficulty_level VARCHAR(20),
    mistakes_count INTEGER DEFAULT 0,

    played_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_brain_game_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE INDEX idx_brain_game_user_id ON brain_game_results(user_id);

COMMENT ON TABLE brain_game_results IS 'Результаты тренировок памяти и внимания';