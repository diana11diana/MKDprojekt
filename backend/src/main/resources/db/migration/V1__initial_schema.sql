CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(32),
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE class_sessions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    dance_style VARCHAR(100) NOT NULL,
    level VARCHAR(20) NOT NULL,
    instructor_name VARCHAR(200) NOT NULL,
    capacity INT NOT NULL,
    booked_places INT NOT NULL DEFAULT 0,
    start_at DATETIME(6) NOT NULL,
    duration_minutes INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_class_sessions_schedule (status, start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reservations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    class_session_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reservation_user_class (user_id, class_session_id),
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reservation_class FOREIGN KEY (class_session_id) REFERENCES class_sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO class_sessions
    (title, description, dance_style, level, instructor_name, capacity, booked_places, start_at, duration_minutes, status)
VALUES
    ('Salsa Start', 'Основы сальсы для начинающих', 'Salsa', 'BEGINNER', 'Анна Ковальска', 16, 9, UTC_TIMESTAMP(6) + INTERVAL 1 DAY, 60, 'PUBLISHED'),
    ('Bachata Flow', 'Пластика и музыкальность', 'Bachata', 'INTERMEDIATE', 'Марк Новак', 14, 12, UTC_TIMESTAMP(6) + INTERVAL 2 DAY, 75, 'PUBLISHED'),
    ('Contemporary', 'Техника contemporary для всех уровней', 'Contemporary', 'ALL', 'Диана Вишневская', 18, 6, UTC_TIMESTAMP(6) + INTERVAL 3 DAY, 90, 'PUBLISHED');

