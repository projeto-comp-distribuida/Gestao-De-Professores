#!/bin/bash
set -e

# Cria o banco se não existir
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
    SELECT 'CREATE DATABASE distrischool_teachers'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'distrischool_teachers')\gexec
EOSQL

echo "Database distrischool_teachers verified/created"

