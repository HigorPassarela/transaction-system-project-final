# Sistema de Transações Bancárias - Agibank

Sistema completo para processamento de transações bancárias (crédito/débito) com notificações em tempo real.

## Arquitetura

- **Kafka**: Mensageria entre serviços
- **Redis**: Cache de saldos e limites
- **MongoDB**: Persistência de extratos
- **Camunda**: Orquestração de workflows
- **3 Microserviços**: Transações, Extratos e Notificações

## Tecnologias

- Java 21
- Spring Boot 3.2.0
- Gradle 8.5
- Apache Kafka
- Redis
- MongoDB
- Camunda BPM
- Docker & Docker Compose
- Swagger/OpenAPI

## Fluxo Principal

1. Cliente solicita transação (crédito/débito)
2. Sistema verifica saldo/limite no Redis
3. Transação aprovada é publicada no Kafka
4. Workflow Camunda processa e salva extrato
5. Cliente recebe notificação em tempo real via SSE

## Como Executar

1. Subir infraestrutura: `docker-compose up -d`
2. Executar serviços individualmente
3. Acessar Swagger: http://localhost:808X/swagger-ui.html

## Serviços

- **Transaction Service**: http://localhost:8081
- **Extract Service**: http://localhost:8082
- **Notifications Service**: http://localhost:8083
- **Camunda**: http://localhost:8080
- **Frontend**: http://localhost:3000
