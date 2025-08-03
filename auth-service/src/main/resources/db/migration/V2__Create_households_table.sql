-- V2__Create_households_table.sql
-- Vytvoření tabulky domácností a propojení s uživateli

CREATE TABLE households (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    invite_code VARCHAR(9) NOT NULL UNIQUE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    max_members INTEGER DEFAULT 10 NOT NULL
);

-- Indexy
CREATE INDEX idx_households_invite_code ON households(invite_code);
CREATE INDEX idx_households_created_by ON households(created_by);
CREATE INDEX idx_households_active ON households(is_active);

-- Foreign key constraints
ALTER TABLE users
ADD CONSTRAINT fk_users_household
FOREIGN KEY (household_id) REFERENCES households(id);

ALTER TABLE households
ADD CONSTRAINT fk_households_creator
FOREIGN KEY (created_by) REFERENCES users(id);

-- Komentáře
COMMENT ON TABLE households IS 'Domácnosti v aplikaci HomeBuddy';
COMMENT ON COLUMN households.invite_code IS 'Kód pro pozvání do domácnosti (formát: ABCD-1234)';
COMMENT ON COLUMN households.created_by IS 'ID uživatele, který domácnost vytvořil';
COMMENT ON COLUMN households.max_members IS 'Maximální počet členů domácnosti';