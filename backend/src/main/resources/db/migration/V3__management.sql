CREATE TABLE instructor_profiles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    specialization VARCHAR(255) NOT NULL,
    description TEXT NULL,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_instructor_profiles_user (user_id),
    CONSTRAINT fk_instructor_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE class_sessions
    ADD COLUMN instructor_id BIGINT UNSIGNED NULL AFTER instructor_name,
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER status,
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at,
    ADD CONSTRAINT fk_class_sessions_instructor
        FOREIGN KEY (instructor_id) REFERENCES instructor_profiles (id);

CREATE INDEX idx_class_sessions_instructor_start
    ON class_sessions (instructor_id, start_at);

