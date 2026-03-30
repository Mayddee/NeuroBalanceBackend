-- User Rewards Table
CREATE TABLE IF NOT EXISTS user_rewards (
                                            id BIGSERIAL PRIMARY KEY,
                                            user_id BIGINT NOT NULL,
                                            reward_type VARCHAR(50) NOT NULL,

    is_unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    unlocked_at TIMESTAMP,
    xp_multiplier DOUBLE PRECISION DEFAULT 1.0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reward_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_reward_type UNIQUE (user_id, reward_type)
    );

CREATE INDEX idx_user_rewards_user_id ON user_rewards(user_id);
CREATE INDEX idx_user_rewards_unlocked ON user_rewards(is_unlocked);