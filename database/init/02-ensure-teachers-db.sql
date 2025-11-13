-- Garante que o banco distrischool_teachers existe
-- Este script será executado sempre que o container for iniciado

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'distrischool_teachers') THEN
        CREATE DATABASE distrischool_teachers;
    END IF;
END
$$;

