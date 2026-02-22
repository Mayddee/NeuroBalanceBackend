

-- Mood Logs Table (детальные записи настроения в течение дня)
CREATE TABLE mood_logs (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT NOT NULL,
                           log_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Mood details
                           mood_value INTEGER NOT NULL CHECK (mood_value BETWEEN 1 AND 5),
                           mood_emoji VARCHAR(10),
                           mood_label VARCHAR(50), -- happy, sad, anxious, stressed, excited, angry, calm, tired

    -- Intensity
                           intensity INTEGER CHECK (intensity BETWEEN 1 AND 10), -- насколько сильно

    -- Context
                           context_note TEXT,
                           location VARCHAR(100), -- home, work, school, gym, outside, transport
                           activity VARCHAR(100), -- working, studying, exercising, socializing, relaxing

    -- Triggers (что вызвало это настроение)
                           triggers TEXT[], -- array: work_stress, relationship, health, achievement, social

    -- Physical sensations
                           physical_sensations TEXT[], -- headache, tension, energy, fatigue, restlessness

                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_mood_logs_user FOREIGN KEY (user_id)
                               REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_mood_logs_user_id ON mood_logs(user_id);
CREATE INDEX idx_mood_logs_timestamp ON mood_logs(log_timestamp DESC);
CREATE INDEX idx_mood_logs_user_timestamp ON mood_logs(user_id, log_timestamp DESC);
CREATE INDEX idx_mood_logs_mood_value ON mood_logs(mood_value);

-- Comments
COMMENT ON TABLE mood_logs IS 'Detailed mood tracking throughout the day';
COMMENT ON COLUMN mood_logs.mood_value IS '1=very bad, 2=bad, 3=neutral, 4=good, 5=very good';
COMMENT ON COLUMN mood_logs.intensity IS 'How strongly user feels this emotion (1-10)';