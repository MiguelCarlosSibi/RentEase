-- Reference DDL for Increment 1 (the backend creates these tables automatically with spring.jpa.hibernate.ddl-auto=update).
-- Database: MySQL 8 / MariaDB (XAMPP)

CREATE DATABASE IF NOT EXISTS rentease CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rentease;

CREATE TABLE IF NOT EXISTS user_account (
    user_id         BIGINT        NOT NULL AUTO_INCREMENT,
    full_name       VARCHAR(100)  NOT NULL,
    email           VARCHAR(100)  NOT NULL,
    mobile_number   VARCHAR(15)   NOT NULL,
    password_hash   VARCHAR(255)  NOT NULL,
    role            ENUM('OWNER','TENANT')     NOT NULL,
    status          ENUM('ACTIVE','INACTIVE')  NOT NULL DEFAULT 'ACTIVE',
    failed_attempts INT           NOT NULL DEFAULT 0,
    locked_until    DATETIME      NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_account_email (email),
    CONSTRAINT chk_failed_attempts CHECK (failed_attempts >= 0)
) ENGINE=InnoDB;

-- Tokens ended by logout; rejected until they would have expired.
CREATE TABLE IF NOT EXISTS revoked_token (
    token_id   VARCHAR(64) NOT NULL,
    expires_at DATETIME    NOT NULL,
    PRIMARY KEY (token_id)
) ENGINE=InnoDB;
