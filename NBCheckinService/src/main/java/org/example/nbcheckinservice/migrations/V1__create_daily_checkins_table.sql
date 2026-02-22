-- V1__create_daily_checkins_table.sql

-- Daily Check-ins Table
CREATE TABLE daily_check_ins (
                                 id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT NOT NULL,
                                 check_in_date DATE NOT NULL,

    -- Mood (1-5 smileys: 😊😐😢😰😡)
                                 morning_mood INTEGER CHECK (morning_mood BETWEEN 1 AND 5),
                                 evening_mood INTEGER CHECK (evening_mood BETWEEN 1 AND 5),
                                 morning_mood_emoji VARCHAR(10),
                                 evening_mood_emoji VARCHAR(10),

    -- Sleep metrics
                                 sleep_quality INTEGER CHECK (sleep_quality BETWEEN 1 AND 10),
                                 sleep_hours DECIMAL(3,1) CHECK (sleep_hours >= 0 AND sleep_hours <= 24),
                                 sleep_bedtime TIME,
                                 sleep_waketime TIME,

    -- Energy & Stress (1-10 scale)
                                 energy_level INTEGER CHECK (energy_level BETWEEN 1 AND 10),
                                 stress_level INTEGER CHECK (stress_level BETWEEN 1 AND 10),

    -- Physical Activity
                                 physical_activity_minutes INTEGER DEFAULT 0 CHECK (physical_activity_minutes >= 0),
                                 physical_activity_type VARCHAR(50), -- walk, gym, yoga, sports, none

    -- Quick Yes/No questions
                                 did_exercise BOOLEAN DEFAULT FALSE,
                                 ate_healthy BOOLEAN DEFAULT FALSE,
                                 had_social_interaction BOOLEAN DEFAULT FALSE,

    -- Cognitive games
                                 played_cognitive_game_today BOOLEAN DEFAULT FALSE,
                                 cognitive_game_count INTEGER DEFAULT 0,

    -- Metadata
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
                                 CONSTRAINT unique_user_date UNIQUE (user_id, check_in_date)
);

-- Indexes for better query performance
CREATE INDEX idx_daily_checkins_user_id ON daily_check_ins(user_id);
CREATE INDEX idx_daily_checkins_date ON daily_check_ins(check_in_date);
CREATE INDEX idx_daily_checkins_user_date ON daily_check_ins(user_id, check_in_date);
CREATE INDEX idx_daily_checkins_created_at ON daily_check_ins(created_at);

-- Comments
COMMENT ON TABLE daily_check_ins IS 'Daily check-in records for user mental health tracking';
COMMENT ON COLUMN daily_check_ins.morning_mood IS '1=😡 2=😰 3=😢 4=😐 5=😊';
COMMENT ON COLUMN daily_check_ins.sleep_quality IS 'Sleep quality on scale 1-10';
COMMENT ON COLUMN daily_check_ins.played_cognitive_game_today IS 'Whether user played any cognitive game today';