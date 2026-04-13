CREATE TABLE IF NOT EXISTS new_game_sessions (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 user_id BIGINT NOT NULL,
                                                 game_type VARCHAR(50) NOT NULL, -- Хранит Enum: DONUT_GAME, NUMBER_SEQUENCE_GAME

    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    is_won BOOLEAN NOT NULL DEFAULT FALSE,
    duration_seconds INTEGER,

    xp_earned INTEGER NOT NULL DEFAULT 0,
    bonus_xp INTEGER DEFAULT 0,

    played_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_new_game_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_duration_new CHECK (duration_seconds >= 0)
    );

CREATE INDEX idx_new_game_user_id ON new_game_sessions(user_id);
CREATE INDEX idx_new_game_type ON new_game_sessions(game_type);

COMMENT ON TABLE new_game_sessions IS 'Развлекательные игры (gamification)';