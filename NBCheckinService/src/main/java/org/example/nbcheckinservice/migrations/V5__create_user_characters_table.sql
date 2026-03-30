-- -- User Characters Table
-- CREATE TABLE IF NOT EXISTS user_characters (
--                                                user_id BIGINT PRIMARY KEY, -- Теперь это и ID записи, и связь с юзером
--
--                                                character_type VARCHAR(20) NOT NULL DEFAULT 'MOA',
--     character_name VARCHAR(50),
--
--     current_level INTEGER NOT NULL DEFAULT 1,
--     total_xp INTEGER NOT NULL DEFAULT 0,
--     xp_for_next_level INTEGER NOT NULL DEFAULT 500,
--
--     happiness_level INTEGER NOT NULL DEFAULT 50,
--     energy_level INTEGER NOT NULL DEFAULT 100,
--
--     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     last_interaction_at TIMESTAMP,
--
--     -- Внешний ключ на таблицу пользователей
--     CONSTRAINT fk_character_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
--
--     -- Проверки (Constraints)
--     CONSTRAINT chk_level CHECK (current_level >= 1 AND current_level <= 5),
--     CONSTRAINT chk_happiness CHECK (happiness_level >= 0 AND happiness_level <= 100),
--     CONSTRAINT chk_energy CHECK (energy_level >= 0 AND energy_level <= 100)
--     );
--
--
-- CREATE INDEX idx_user_characters_level ON user_characters(current_level);
--
CREATE TABLE IF NOT EXISTS user_characters (
                                               id BIGSERIAL PRIMARY KEY,

                                               user_id BIGINT NOT NULL UNIQUE,

                                               character_type VARCHAR(20) NOT NULL DEFAULT 'MOA',
    character_name VARCHAR(50),

    current_level INTEGER NOT NULL DEFAULT 1,
    total_xp INTEGER NOT NULL DEFAULT 0,
    xp_for_next_level INTEGER NOT NULL DEFAULT 500,

    happiness_level INTEGER NOT NULL DEFAULT 50,
    energy_level INTEGER NOT NULL DEFAULT 100,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_interaction_at TIMESTAMP,

    CONSTRAINT fk_character_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT chk_level CHECK (current_level >= 1 AND current_level <= 5),
    CONSTRAINT chk_happiness CHECK (happiness_level >= 0 AND happiness_level <= 100),
    CONSTRAINT chk_energy CHECK (energy_level >= 0 AND energy_level <= 100)
    );

CREATE INDEX idx_user_characters_level ON user_characters(current_level);