-- ===============================================================
-- NOTE-AI v2: Enhanced journal entries table
-- Добавляем расширенную таблицу журнала рядом со старой notes.
-- Старая схема (notes, note_users, note_users_notes) НЕ затронута.
-- ===============================================================

CREATE TABLE IF NOT EXISTS journal_entries (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    title       VARCHAR(500),
    content     TEXT,

    -- Wellness context
    mood_score  INTEGER       CHECK (mood_score >= 1 AND mood_score <= 5),
    tags        VARCHAR(500),          -- comma-separated, e.g. "стресс,работа,сон"
    is_favorite BOOLEAN       NOT NULL DEFAULT FALSE,
    word_count  INTEGER       NOT NULL DEFAULT 0,

    -- Timestamps
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Fast lookups by user + time (для today/range запросов)
CREATE INDEX idx_je_user_created  ON journal_entries (user_id, created_at DESC);
CREATE INDEX idx_je_user_updated  ON journal_entries (user_id, updated_at DESC);
CREATE INDEX idx_je_user_favorite ON journal_entries (user_id, is_favorite);

-- Auto-update updated_at on row change
CREATE OR REPLACE FUNCTION update_journal_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_journal_updated_at
BEFORE UPDATE ON journal_entries
FOR EACH ROW EXECUTE FUNCTION update_journal_updated_at();
