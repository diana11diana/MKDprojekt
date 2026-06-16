CREATE TABLE pass_types (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    type VARCHAR(20) NOT NULL,
    visit_count SMALLINT UNSIGNED NULL,
    validity_days SMALLINT UNSIGNED NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'PLN',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_passes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    pass_type_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL,
    remaining_visits SMALLINT UNSIGNED NULL,
    valid_from DATETIME(6) NOT NULL,
    valid_until DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_user_passes_lookup (user_id, status, valid_until),
    CONSTRAINT fk_user_passes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_passes_type FOREIGN KEY (pass_type_id) REFERENCES pass_types (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE reservations
    ADD COLUMN user_pass_id BIGINT UNSIGNED NULL AFTER class_session_id,
    ADD COLUMN cancelled_at DATETIME(6) NULL AFTER status,
    ADD CONSTRAINT fk_reservations_user_pass
        FOREIGN KEY (user_pass_id) REFERENCES user_passes (id);

CREATE INDEX idx_reservations_class_status
    ON reservations (class_session_id, status);

CREATE TABLE waiting_list (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    class_session_id BIGINT UNSIGNED NOT NULL,
    position INT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_waiting_user_class (user_id, class_session_id),
    UNIQUE KEY uk_waiting_class_position (class_session_id, position),
    KEY idx_waiting_next (class_session_id, status, position),
    CONSTRAINT fk_waiting_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_waiting_class FOREIGN KEY (class_session_id) REFERENCES class_sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pass_ledger_entries (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_pass_id BIGINT UNSIGNED NOT NULL,
    reservation_id BIGINT UNSIGNED NULL,
    type VARCHAR(30) NOT NULL,
    visit_delta SMALLINT NOT NULL,
    balance_after SMALLINT UNSIGNED NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_pass_ledger_pass (user_pass_id, created_at),
    CONSTRAINT fk_pass_ledger_pass FOREIGN KEY (user_pass_id) REFERENCES user_passes (id),
    CONSTRAINT fk_pass_ledger_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO pass_types
    (name, description, type, visit_count, validity_days, price, currency, active)
VALUES
    ('4 занятия', 'Абонемент для знакомства со школой', 'LIMITED', 4, 30, 160.00, 'PLN', TRUE),
    ('8 занятий', 'Регулярные занятия в течение месяца', 'LIMITED', 8, 45, 280.00, 'PLN', TRUE),
    ('Безлимит 30', 'Неограниченное количество занятий на 30 дней', 'UNLIMITED', NULL, 30, 390.00, 'PLN', TRUE);

