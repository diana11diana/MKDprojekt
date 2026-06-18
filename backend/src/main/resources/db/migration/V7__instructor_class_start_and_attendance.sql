ALTER TABLE class_sessions
    ADD COLUMN started_at DATETIME(6) NULL AFTER duration_minutes;
