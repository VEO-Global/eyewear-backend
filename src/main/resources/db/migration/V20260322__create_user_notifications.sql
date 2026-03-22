CREATE TABLE user_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(255) NULL,
    message LONGTEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    source_module VARCHAR(50) NOT NULL,
    metadata_json JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    CONSTRAINT chk_user_notification_type
        CHECK (type IN ('success', 'error', 'warning', 'info')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_notifications_user_id
    ON user_notifications (user_id);

CREATE INDEX idx_user_notifications_user_read_created
    ON user_notifications (user_id, is_read, created_at);

CREATE INDEX idx_user_notifications_user_created
    ON user_notifications (user_id, created_at);

CREATE INDEX idx_user_notifications_expires_at
    ON user_notifications (expires_at);
