#!/bin/bash

# Script para executar testes de integração no Docker
# Uso: ./scripts/run-integration-tests.sh

set -e

echo "🚀 Executando testes de integração no Docker..."
echo ""

# Verifica se o docker-compose está instalado
if ! command -v docker-compose &> /dev/null; then
    echo "❌ docker-compose não encontrado. Instale o Docker Compose primeiro."
    exit 1
fi

# Build da imagem de testes
echo "📦 Construindo imagem de testes..."
docker-compose build --target test integration-tests

# Executa os testes
echo "🧪 Executando testes de integração..."
docker-compose --profile test run --rm integration-tests

# Verifica o código de saída
EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "✅ Todos os testes passaram com sucesso!"
else
    echo ""
    echo "❌ Alguns testes falharam. Código de saída: $EXIT_CODE"
fi

exit $EXIT_CODE

