-- V4__create_sleep_logs_table.sql

-- Sleep Logs Table (детальная информация о сне)
CREATE TABLE sleep_logs (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT NOT NULL,
                            sleep_date DATE NOT NULL, -- дата сна (день когда лёг спать)

    -- Sleep timing
                            bedtime TIMESTAMP,
                            wake_time TIMESTAMP,
                            fell_asleep_time TIMESTAMP, -- когда реально заснул (может быть позже bedtime)

    -- Duration
                            total_hours DECIMAL(4,2), -- общее время в кровати
                            actual_sleep_hours DECIMAL(4,2), -- реальное время сна
                            time_to_fall_asleep_minutes INTEGER, -- сколько времени уснуть

    -- Quality
                            quality_score INTEGER CHECK (quality_score BETWEEN 1 AND 10),
                            sleep_efficiency DECIMAL(5,2), -- процент времени реально спал (actual / total * 100)
                            felt_rested BOOLEAN, -- чувствует ли себя отдохнувшим

    -- Interruptions
                            interruptions_count INTEGER DEFAULT 0,
                            awake_duration_minutes INTEGER DEFAULT 0, -- сколько был проснулся ночью
                            bathroom_trips INTEGER DEFAULT 0,

    -- Sleep stages (optional, for advanced tracking)
                            deep_sleep_minutes INTEGER,
                            light_sleep_minutes INTEGER,
                            rem_sleep_minutes INTEGER,
                            awake_minutes INTEGER,

    -- Dreams
                            had_dreams BOOLEAN,
                            dream_recall VARCHAR(20), -- none, vague, clear, vivid
                            dream_notes TEXT,
                            nightmares BOOLEAN DEFAULT FALSE,

    -- Environment
                            room_temperature VARCHAR(20), -- cold, comfortable, warm, hot
                            noise_level VARCHAR(20), -- silent, quiet, moderate, noisy
                            light_level VARCHAR(20), -- dark, dim, bright
                            bed_comfort VARCHAR(20), -- uncomfortable, okay, comfortable, very_comfortable

    -- Pre-sleep factors
                            caffeine_before_bed BOOLEAN,
                            screen_time_before_bed_minutes INTEGER,
                            exercise_before_bed BOOLEAN,
                            alcohol BOOLEAN,
                            heavy_meal BOOLEAN,

    -- Morning feeling
                            morning_mood INTEGER CHECK (morning_mood BETWEEN 1 AND 5),
                            morning_energy INTEGER CHECK (morning_energy BETWEEN 1 AND 10),

    -- Notes
                            notes TEXT,

                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            UNIQUE(user_id, sleep_date),

                            CONSTRAINT fk_sleep_logs_user FOREIGN KEY (user_id)
                                REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_sleep_logs_user_id ON sleep_logs(user_id);
CREATE INDEX idx_sleep_logs_date ON sleep_logs(sleep_date DESC);
CREATE INDEX idx_sleep_logs_user_date ON sleep_logs(user_id, sleep_date DESC);
CREATE INDEX idx_sleep_logs_quality ON sleep_logs(quality_score);

-- Comments
COMMENT ON TABLE sleep_logs IS 'Detailed sleep tracking with environment and quality metrics';
COMMENT ON COLUMN sleep_logs.sleep_efficiency IS 'Percentage of time actually sleeping (actual_sleep / total_hours * 100)';
COMMENT ON COLUMN sleep_logs.fell_asleep_time IS 'Actual time user fell asleep (may differ from bedtime)';›