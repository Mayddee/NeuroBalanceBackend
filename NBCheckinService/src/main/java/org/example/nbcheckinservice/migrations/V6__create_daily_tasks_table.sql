-- Daily Tasks Table
CREATE TABLE IF NOT EXISTS daily_tasks (
                                           id BIGSERIAL PRIMARY KEY,
                                           user_id BIGINT NOT NULL,
                                           task_date DATE NOT NULL,
                                           task_type VARCHAR(50) NOT NULL,

    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    xp_reward INTEGER NOT NULL DEFAULT 50,

    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_task_date UNIQUE (user_id, task_date, task_type)
    );

CREATE INDEX idx_daily_tasks_user_date ON daily_tasks(user_id, task_date);
CREATE INDEX idx_daily_tasks_completion ON daily_tasks(is_completed);