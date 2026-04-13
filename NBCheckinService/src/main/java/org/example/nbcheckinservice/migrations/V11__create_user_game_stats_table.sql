CREATE TABLE IF NOT EXISTS user_game_stats (
                                               id BIGSERIAL PRIMARY KEY,
                                               user_id BIGINT NOT NULL UNIQUE,

                                               total_games_played INTEGER NOT NULL DEFAULT 0,
                                               total_xp_earned INTEGER NOT NULL DEFAULT 0,
                                               total_wins INTEGER NOT NULL DEFAULT 0,
                                               total_losses INTEGER NOT NULL DEFAULT 0,


                                               number_sequence_best_time INTEGER,
                                               memory_pairs_best_time INTEGER,

                                               current_streak INTEGER NOT NULL DEFAULT 0,
                                               best_streak INTEGER NOT NULL DEFAULT 0,
                                               last_played_date DATE, -- LocalDate в Java

                                               CONSTRAINT fk_stats_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE INDEX idx_user_stats_user_id ON user_game_stats(user_id);

COMMENT ON TABLE user_game_stats IS 'Общая игровая статистика и рекорды игрока';