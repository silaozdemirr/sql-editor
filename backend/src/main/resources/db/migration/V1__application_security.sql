CREATE TABLE app_users (
    id CHAR(36) PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refresh_tokens (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    INDEX idx_refresh_user (user_id), INDEX idx_refresh_expiry (expires_at)
);

CREATE TABLE connection_history (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    connection_name VARCHAR(100) NULL,
    db_type VARCHAR(20) NOT NULL,
    host VARCHAR(253) NOT NULL,
    port INT NOT NULL,
    database_name VARCHAR(128) NOT NULL,
    db_username VARCHAR(128) NOT NULL,
    connected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    INDEX idx_history_user_time (user_id, connected_at DESC)
);

CREATE TABLE connection_sessions (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    db_type VARCHAR(20) NOT NULL,
    host VARCHAR(253) NOT NULL,
    port INT NOT NULL,
    database_name VARCHAR(128) NOT NULL,
    db_username VARCHAR(128) NOT NULL,
    password_ciphertext TEXT NOT NULL,
    password_iv VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    INDEX idx_session_expiry (expires_at)
);
