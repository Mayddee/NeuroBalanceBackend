-- Game Sessions Table (Fun Games for Gamification)
CREATE TABLE IF NOT EXISTS game_sessions (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id BIGINT NOT NULL,
                                             game_type VARCHAR(50) NOT NULL,

    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    is_won BOOLEAN NOT NULL DEFAULT FALSE,
    duration_seconds INTEGER,

    xp_earned INTEGER NOT NULL DEFAULT 0,
    bonus_xp INTEGER,

    played_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_game_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_duration CHECK (duration_seconds >= 0)
    );

CREATE INDEX idx_game_sessions_user_id ON game_sessions(user_id);
CREATE INDEX idx_game_sessions_played_at ON game_sessions(played_at);
CREATE INDEX idx_game_sessions_game_type ON game_sessions(game_type);

COMMENT ON TABLE game_sessions IS 'Fun gamification games (not cognitive tests)';
COMMENT ON COLUMN game_sessions.game_type IS 'DONUT_GAME or CHARACTER_CARE';