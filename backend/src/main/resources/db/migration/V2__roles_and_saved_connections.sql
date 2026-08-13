ALTER TABLE app_users
    ADD COLUMN role_name VARCHAR(20) NOT NULL DEFAULT 'EDITOR';

CREATE TABLE saved_connections (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    connection_name VARCHAR(100) NOT NULL,
    db_type VARCHAR(20) NOT NULL,
    host VARCHAR(253) NOT NULL,
    port INT NOT NULL,
    database_name VARCHAR(128) NOT NULL,
    db_username VARCHAR(128) NOT NULL,
    password_ciphertext TEXT NOT NULL,
    password_iv VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_saved_connection_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT uq_saved_connection_name UNIQUE (user_id, connection_name),
    INDEX idx_saved_connection_user (user_id)
);
