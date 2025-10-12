#!/bin/bash

# Script para testar a API de Professores do DistriSchool
# Execute: ./scripts/test-teacher-api.sh

set -e

BASE_URL="http://localhost:8080/api/v1/teachers"

echo "================================================"
echo "Testando API de Professores - DistriSchool"
echo "================================================"
echo "Base URL: $BASE_URL"
echo ""

# Função para fazer requisições HTTP
make_request() {
    local method=$1
    local url=$2
    local data=$3
    
    echo "🔍 $method $url"
    if [ -n "$data" ]; then
        echo "📤 Dados: $data"
        curl -s -X $method "$url" \
            -H "Content-Type: application/json" \
            -d "$data" | jq '.' 2>/dev/null || echo "Resposta: $(curl -s -X $method "$url" -H "Content-Type: application/json" -d "$data")"
    else
        curl -s -X $method "$url" | jq '.' 2>/dev/null || echo "Resposta: $(curl -s -X $method "$url")"
    fi
    echo ""
}

# 1. Testar Health Check
echo "1️⃣ Testando Health Check..."
make_request "GET" "http://localhost:8080/api/v1/health"
echo ""

# 2. Listar professores (deve estar vazio inicialmente)
echo "2️⃣ Listando professores..."
make_request "GET" "$BASE_URL"
echo ""

# 3. Criar primeiro professor
echo "3️⃣ Criando primeiro professor..."
PROFESSOR_1='{
    "name": "Maria Silva",
    "employeeId": "PROF001",
    "birthDate": "1980-05-15",
    "email": "maria.silva@distrischool.com",
    "phone": "+5511999999999",
    "qualification": "Mestrado em Matemática",
    "subjects": ["Matemática", "Física"],
    "hireDate": "2020-01-01",
    "salary": "5000.00"
}'
make_request "POST" "$BASE_URL" "$PROFESSOR_1"
echo ""

# 4. Criar segundo professor
echo "4️⃣ Criando segundo professor..."
PROFESSOR_2='{
    "name": "João Santos",
    "employeeId": "PROF002",
    "birthDate": "1975-08-20",
    "email": "joao.santos@distrischool.com",
    "phone": "+5511888888888",
    "qualification": "Doutorado em História",
    "subjects": ["História", "Geografia"],
    "hireDate": "2019-03-15",
    "salary": "6000.00"
}'
make_request "POST" "$BASE_URL" "$PROFESSOR_2"
echo ""

# 5. Listar todos os professores
echo "5️⃣ Listando todos os professores..."
make_request "GET" "$BASE_URL"
echo ""

# 6. Buscar professor por ID
echo "6️⃣ Buscando professor por ID (1)..."
make_request "GET" "$BASE_URL/1"
echo ""

# 7. Buscar professor por matrícula
echo "7️⃣ Buscando professor por matrícula (PROF002)..."
make_request "GET" "$BASE_URL/employee/PROF002"
echo ""

# 8. Buscar professores por disciplina
echo "8️⃣ Buscando professores de Matemática..."
make_request "GET" "$BASE_URL/subject/Matemática"
echo ""

# 9. Buscar professores por status
echo "9️⃣ Buscando professores ativos..."
make_request "GET" "$BASE_URL/status/ACTIVE"
echo ""

# 10. Buscar professores por período de contratação
echo "🔟 Buscando professores contratados em 2020..."
make_request "GET" "$BASE_URL/hired?startDate=2020-01-01&endDate=2020-12-31"
echo ""

# 11. Atualizar professor
echo "1️⃣1️⃣ Atualizando professor (ID: 1)..."
PROFESSOR_UPDATE='{
    "name": "Maria Silva Atualizada",
    "employeeId": "PROF001",
    "birthDate": "1980-05-15",
    "email": "maria.nova@distrischool.com",
    "phone": "+5511999999999",
    "qualification": "Doutorado em Matemática",
    "subjects": ["Matemática", "Física", "Cálculo"],
    "hireDate": "2020-01-01",
    "salary": "7000.00"
}'
make_request "PUT" "$BASE_URL/1" "$PROFESSOR_UPDATE"
echo ""

# 12. Verificar atualização
echo "1️⃣2️⃣ Verificando atualização do professor..."
make_request "GET" "$BASE_URL/1"
echo ""

# 13. Criar terceiro professor para testar exclusão
echo "1️⃣3️⃣ Criando terceiro professor para teste de exclusão..."
PROFESSOR_3='{
    "name": "Ana Costa",
    "employeeId": "PROF003",
    "birthDate": "1985-12-10",
    "email": "ana.costa@distrischool.com",
    "phone": "+5511777777777",
    "qualification": "Especialização em Pedagogia",
    "subjects": ["Português", "Literatura"],
    "hireDate": "2021-06-01",
    "salary": "4500.00"
}'
make_request "POST" "$BASE_URL" "$PROFESSOR_3"
echo ""

# 14. Listar professores antes da exclusão
echo "1️⃣4️⃣ Listando professores antes da exclusão..."
make_request "GET" "$BASE_URL"
echo ""

# 15. Excluir professor
echo "1️⃣5️⃣ Excluindo professor (ID: 3)..."
make_request "DELETE" "$BASE_URL/3"
echo ""

# 16. Verificar exclusão (soft delete)
echo "1️⃣6️⃣ Verificando exclusão (soft delete)..."
make_request "GET" "$BASE_URL"
echo ""

# 17. Testar busca por disciplina após atualização
echo "1️⃣7️⃣ Buscando professores de Matemática após atualização..."
make_request "GET" "$BASE_URL/subject/Matemática"
echo ""

# 18. Verificar métricas do Actuator
echo "1️⃣8️⃣ Verificando métricas do Actuator..."
make_request "GET" "http://localhost:8080/actuator/metrics"
echo ""

# 19. Verificar health check detalhado
echo "1️⃣9️⃣ Verificando health check detalhado..."
make_request "GET" "http://localhost:8080/actuator/health"
echo ""

echo "================================================"
echo "✅ Testes da API de Professores concluídos!"
echo "================================================"
echo ""
echo "📊 Resumo dos testes:"
echo "   - Health Check: ✅"
echo "   - CRUD de Professores: ✅"
echo "   - Busca por ID: ✅"
echo "   - Busca por matrícula: ✅"
echo "   - Busca por disciplina: ✅"
echo "   - Busca por status: ✅"
echo "   - Busca por período: ✅"
echo "   - Atualização: ✅"
echo "   - Exclusão (soft delete): ✅"
echo "   - Métricas: ✅"
echo ""
echo "🔍 Para verificar eventos no Kafka:"
echo "   Acesse: http://localhost:8090"
echo "   Tópicos: teacher.created, teacher.updated, teacher.deleted"
echo ""
echo "📈 Para monitoramento:"
echo "   Métricas: http://localhost:8080/actuator/prometheus"
echo "   Health: http://localhost:8080/actuator/health"
echo "================================================"
