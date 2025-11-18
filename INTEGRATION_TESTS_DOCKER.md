# Como Executar Testes de Integração no Docker

Este documento explica como executar os testes de integração dentro de um container Docker.

## Pré-requisitos

- Docker e Docker Compose instalados
- Acesso à internet para baixar dependências Maven

## Métodos de Execução

### Método 1: Usando o Script Automatizado (Recomendado)

```bash
./scripts/run-integration-tests.sh
```

### Método 2: Usando Docker Compose

```bash
# Build e execução dos testes
docker-compose --profile test build integration-tests
docker-compose --profile test run --rm integration-tests
```

### Método 3: Usando Docker Diretamente

```bash
# Build da imagem de testes
docker build --target test -t distrischool-integration-tests .

# Executa todos os testes
docker run --rm distrischool-integration-tests

# Executa apenas testes de integração específicos
docker run --rm distrischool-integration-tests mvn test -Dtest=TeacherManagementControllerIntegrationTest

# Executa com mais memória
docker run --rm -e MAVEN_OPTS="-Xmx2048m" distrischool-integration-tests
```

### Método 4: Executar Testes Específicos

```bash
# Executa apenas uma classe de teste
docker-compose --profile test run --rm integration-tests mvn test -Dtest=TeacherManagementControllerIntegrationTest

# Executa testes em um pacote específico
docker-compose --profile test run --rm integration-tests mvn test -Dtest=com.distrischool.template.integration.*

# Executa com coverage
docker-compose --profile test run --rm integration-tests mvn test jacoco:report
```

## Verificar Resultados

Os resultados dos testes são exibidos no console. Para salvar os resultados:

```bash
# Salva o output em um arquivo
docker-compose --profile test run --rm integration-tests > test-results.txt 2>&1

# Acessa os relatórios gerados (se configurado)
docker-compose --profile test run --rm integration-tests mvn test surefire-report:report
```

## Variáveis de Ambiente

Você pode customizar o comportamento dos testes usando variáveis de ambiente:

```bash
docker-compose --profile test run --rm \
  -e SPRING_PROFILES_ACTIVE=test \
  -e MAVEN_OPTS="-Xmx2048m" \
  integration-tests
```

## Troubleshooting

### Erro: "Out of memory"
Aumente a memória disponível:
```bash
docker-compose --profile test run --rm \
  -e MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m" \
  integration-tests
```

### Erro: "Tests failed"
Verifique os logs:
```bash
docker-compose --profile test logs integration-tests
```

### Limpar cache do Maven
```bash
docker volume rm gestao-de-professores_maven-cache
```

### Executar testes em modo debug
```bash
docker-compose --profile test run --rm \
  -e MAVEN_OPTS="-Xmx1024m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005" \
  -p 5005:5005 \
  integration-tests
```

## Arquitetura dos Testes

Os testes de integração são configurados para usar:
- **H2 Database**: Banco de dados em memória para testes rápidos
- **Embedded Kafka**: Kafka embutido para testes de mensageria
- **Security Disabled**: Segurança desabilitada para facilitar testes
- **Transactional**: Cada teste roda em uma transação que é revertida ao final

## Testes Incluídos

1. **TeacherManagementControllerIntegrationTest**: Testes de API REST
2. **TeacherManagementServiceIntegrationTest**: Testes de serviços
3. **KafkaIntegrationTest**: Testes de eventos Kafka
4. **TeacherRepositoryIntegrationTest**: Testes de repositório

## CI/CD Integration

Para usar em pipelines CI/CD:

```yaml
# Exemplo GitHub Actions
- name: Run Integration Tests
  run: |
    docker-compose --profile test build integration-tests
    docker-compose --profile test run --rm integration-tests
```







