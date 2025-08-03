-- V1__Create_users_table.sql
-- Vytvoření tabulky pro uživatele

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    household_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE NOT NULL
);

-- Indexy pro rychlejší vyhledávání
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_household_id ON users(household_id);
CREATE INDEX idx_users_active ON users(is_active);

-- Komentáře
COMMENT ON TABLE users IS 'Uživatelé aplikace HomeBuddy';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hash hesla';
COMMENT ON COLUMN users.household_id IS 'FK na household - může být NULL';
COMMENT ON COLUMN users.is_active IS 'Soft delete flag';