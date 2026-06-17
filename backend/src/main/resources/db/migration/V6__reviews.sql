CREATE TABLE class_reviews (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    class_session_id BIGINT UNSIGNED NOT NULL,
    rating TINYINT UNSIGNED NOT NULL,
    comment VARCHAR(2000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_class_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_class_reviews_class FOREIGN KEY (class_session_id) REFERENCES class_sessions (id),
    CONSTRAINT uk_class_reviews_user_class UNIQUE (user_id, class_session_id),
    CONSTRAINT chk_class_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    KEY idx_class_reviews_class_created (class_session_id, created_at),
    KEY idx_class_reviews_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE review_replies (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    review_id BIGINT UNSIGNED NOT NULL,
    author_user_id BIGINT UNSIGNED NOT NULL,
    author_role VARCHAR(20) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_review_replies_review FOREIGN KEY (review_id) REFERENCES class_reviews (id),
    CONSTRAINT fk_review_replies_author FOREIGN KEY (author_user_id) REFERENCES users (id),
    KEY idx_review_replies_review_created (review_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
