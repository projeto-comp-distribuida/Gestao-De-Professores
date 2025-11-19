#!/bin/bash
# Script para criar o banco de dados distrischool_auth
# Este script é executado automaticamente quando o container PostgreSQL é criado

# Verificar se o banco já existe antes de criar
DB_EXISTS=$(psql -v ON_ERROR_STOP=0 -t -A --username "$POSTGRES_USER" --dbname "postgres" -c "SELECT 1 FROM pg_database WHERE datname='distrischool_auth';" 2>/dev/null || echo "")

if [ -z "$DB_EXISTS" ]; then
    echo "Creating database distrischool_auth..."
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
        CREATE DATABASE distrischool_auth;
EOSQL
    echo "Database distrischool_auth created successfully"
else
    echo "Database distrischool_auth already exists"
fi

