-- Inicializační skript pro PostgreSQL
-- Tento soubor se spustí automaticky při prvním vytvoření databáze

-- Nastavení timezone
SET timezone = 'UTC';

-- Vytvoření extenzí (pokud je potřeba)
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Informační komentář
COMMENT ON DATABASE homebuddy IS 'HomeBuddy Application Database';

-- Log inicializace
DO $$
BEGIN
    RAISE NOTICE 'HomeBuddy database initialized successfully at %', NOW();
END
$$;