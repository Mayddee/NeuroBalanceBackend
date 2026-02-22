-- V2__create_user_streaks_table.sql

-- User Streaks Table
CREATE TABLE user_streaks (
                              id BIGSERIAL PRIMARY KEY,
                              user_id BIGINT NOT NULL UNIQUE,

    -- Streak counters
                              current_streak INTEGER DEFAULT 0 CHECK (current_streak >= 0),
                              longest_streak INTEGER DEFAULT 0 CHECK (longest_streak >= 0),

    -- Activity tracking
                              last_checkin_date DATE,
                              total_checkins INTEGER DEFAULT 0 CHECK (total_checkins >= 0),

    -- XP tracking
                              total_xp_earned INTEGER DEFAULT 0 CHECK (total_xp_earned >= 0),

    -- Metadata
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraint
                              CONSTRAINT fk_user_streaks_user FOREIGN KEY (user_id)
                                  REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_user_streaks_user_id ON user_streaks(user_id);
CREATE INDEX idx_user_streaks_current_streak ON user_streaks(current_streak DESC);
CREATE INDEX idx_user_streaks_longest_streak ON user_streaks(longest_streak DESC);

-- Comments
COMMENT ON TABLE user_streaks IS 'User streak tracking for daily check-ins';
COMMENT ON COLUMN user_streaks.current_streak IS 'Number of consecutive days with check-ins';
COMMENT ON COLUMN user_streaks.longest_streak IS 'Maximum streak achieved by user';
COMMENT ON COLUMN user_streaks.total_xp_earned IS 'Total XP earned from check-ins and streaks';