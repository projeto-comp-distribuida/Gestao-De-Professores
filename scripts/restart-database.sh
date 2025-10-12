#!/bin/bash

# Script para reiniciar o banco de dados e aplicar correções
# Execute: ./scripts/restart-database.sh

set -e

echo "================================================"
echo "Reiniciando Banco de Dados - DistriSchool"
echo "================================================"

# Parar containers
echo "🛑 Parando containers..."
docker-compose down

# Remover volumes para limpar dados
echo "🗑️ Removendo volumes antigos..."
docker volume rm gestao-de-professores_postgres_data 2>/dev/null || true
docker volume rm gestao-de-professores_redis_data 2>/dev/null || true

# Subir containers novamente
echo "🚀 Subindo containers..."
docker-compose up -d

# Aguardar PostgreSQL estar pronto
echo "⏳ Aguardando PostgreSQL estar pronto..."
sleep 10

# Verificar se o banco está rodando
echo "🔍 Verificando status dos containers..."
docker-compose ps

# Verificar logs do PostgreSQL
echo "📋 Logs do PostgreSQL:"
docker-compose logs postgres | tail -20

echo ""
echo "================================================"
echo "✅ Banco de dados reiniciado com sucesso!"
echo "================================================"
echo ""
echo "🔧 Próximos passos:"
echo "   1. Executar aplicação: ./mvnw spring-boot:run"
echo "   2. Testar sistema: ./scripts/test-complete-teacher-system.sh"
echo ""
echo "📊 Verificar migrações:"
echo "   docker-compose logs postgres | grep 'V[0-9]__'"
echo ""
echo "🔍 Acessar banco:"
echo "   docker exec -it postgres psql -U distrischool -d distrischool_template"
echo "================================================"
