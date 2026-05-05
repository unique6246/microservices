# Microservices Architecture Graph

```mermaid
flowchart TD
    CLIENT(["🌐 External Client
    HTTP Consumer"])

    subgraph INFRA["⚙️ Infrastructure Layer"]
        direction TB
        EUREKA[["🔍 EurekaServer
        ═══════════════════════
        Role   : [Discovery]
        Job    : Service registry + health monitoring
        Tech   : Spring Cloud Netflix Eureka
        Port   : 8761"]]

        GATEWAY["🚪 ApiGateway
        ═══════════════════════
        Role   : [Gateway]
        Job    : Single entry-point · path-based routing
                 Load-balanced via Spring Cloud Gateway
        Routes : /customers/** /accounts/**
                 /transactions/** /notifications/**
        Port   : 8080"]

        MQ[["📨 RabbitMQ Message Broker
        ═══════════════════════
        Role     : [Async Event Bus]
        Exchange : accountExchange  (TopicExchange)
        Queue    : accountJsonQueue
        Key      : accountJsonRoutingKey
        Port     : 5672"]]
    end

    subgraph BUSINESS["💼 Core Business Services Layer"]
        direction TB
        CUSTOMER["👤 CustomerService
        ═══════════════════════
        Role    : [Core Service]
        Owns    : Customer domain  →  customer_db (MySQL)
        Port    : 8002
        APIs    : POST /customers         — register new customer
                  GET  /customers/{id}    — fetch customer profile
                  GET  /customers         — list all customers
                  DELETE /customers/{id}  — delete customer + account cascade
        Feign   : → AccountService  (get/delete linked account)
        Async   : publishes customer.created / customer.deleted events"]

        ACCOUNT["🏦 AccountService
        ═══════════════════════
        Role    : [Core Service]
        Owns    : Account domain  →  accounts_db (MySQL)
        Port    : 8001
        APIs    : POST   /accounts         — open bank account
                  GET    /accounts/{id}    — get by id
                  GET    /accounts/customer/{id}  — get by customer
                  GET    /accounts/account/{no}   — get by account number
                  PUT    /accounts/update   — update balance
                  DELETE /accounts/{no}    — close account
        Feign   : → CustomerService (fetch email / name for notifications)
        Async   : publishes account.created / account.deleted events"]

        TRANSACTION["💳 TransactionService
        ═══════════════════════
        Role    : [Core Service]
        Owns    : Transaction domain  →  transaction_db (MySQL)
        Port    : 8003
        APIs    : POST /transactions/credit   — credit amount to account
                  POST /transactions/debit    — debit amount from account
                  POST /transactions/transfer — peer-to-peer transfer
                  GET  /transactions          — list all transactions
                  GET  /transactions/{accountNo} — by account
        Feign   : → AccountService  (get/update/validate account balance)
                  → CustomerService (fetch customer email for notification)
        Async   : publishes transaction.credit / debit / transfer events"]
    end

    subgraph SUPPORT["🛠️ Support Services Layer"]
        NOTIFICATION["📧 NotificationService
        ═══════════════════════
        Role     : [Support Service]
        Owns     : Notification log  →  notification_db (MySQL)
        Port     : 8004
        APIs     : POST /notifications/send  — send email synchronously
        Triggers : @RabbitListener on accountJsonQueue (primary path)
                   POST /notifications/send via Feign (fallback/direct)
        Action   : validates email · sends via JavaMailSender (SMTP)
                   persists notification record to DB"]
    end

    %% ── External Entry ──────────────────────────────────────────────
    CLIENT -->|"HTTP request"| GATEWAY

    %% ── Gateway → Services (sync routing) ──────────────────────────
    GATEWAY -->|"routes request  /customers/**
    lb://CUSTOMER-SERVICE"| CUSTOMER
    GATEWAY -->|"routes request  /accounts/**
    lb://ACCOUNT-SERVICE"| ACCOUNT
    GATEWAY -->|"routes request  /transactions/**
    lb://TRANSACTION-SERVICE"| TRANSACTION
    GATEWAY -->|"routes request  /notifications/**
    lb://NOTIFICATION-SERVICE"| NOTIFICATION

    %% ── Eureka Registration (all services) ──────────────────────────
    GATEWAY    -->|"registers with"| EUREKA
    CUSTOMER   -->|"registers with"| EUREKA
    ACCOUNT    -->|"registers with"| EUREKA
    TRANSACTION-->|"registers with"| EUREKA
    NOTIFICATION-->|"registers with"| EUREKA
    GATEWAY    -.->|"discovers services via"| EUREKA

    %% ── Sync Feign Calls (inter-service) ────────────────────────────
    ACCOUNT -->|"calls sync  Feign
    GET /customers/{id}
    fetch name + email"| CUSTOMER

    CUSTOMER -->|"calls sync  Feign
    GET  /accounts/customer/{id}
    DELETE /accounts/{accountNo}
    cascade on customer delete"| ACCOUNT

    TRANSACTION -->|"calls sync  Feign
    GET  /accounts/account/{no}
    GET  /accounts/customer/{id}
    PUT  /accounts/update  balance"| ACCOUNT

    TRANSACTION -->|"calls sync  Feign
    GET /customers/{id}
    resolve email for notification"| CUSTOMER

    %% ── Async Event Publishing → RabbitMQ ───────────────────────────
    ACCOUNT -->|"publishes event  async
    account.created
    account.deleted
    → accountExchange"| MQ

    CUSTOMER -->|"publishes event  async
    customer.created
    customer.deleted
    → accountExchange"| MQ

    TRANSACTION -->|"publishes event  async
    transaction.credit
    transaction.debit
    transaction.transfer
    → accountExchange"| MQ

    %% ── Async Event Consumption ─────────────────────────────────────
    MQ -->|"delivers event  async
    @RabbitListener
    ← accountJsonQueue
    JSON deserialized → NotificationDTO"| NOTIFICATION

    %% ── Feign Clients declared but async path preferred ─────────────
    ACCOUNT -.->|"Feign client declared
    POST /notifications/send
    sync fallback  currently unused"| NOTIFICATION
    CUSTOMER -.->|"Feign client declared
    POST /notifications/send
    sync fallback  currently unused"| NOTIFICATION

    %% ── Styling ─────────────────────────────────────────────────────
    classDef gateway    fill:#f0a500,stroke:#b37400,color:#000,font-weight:bold
    classDef discovery  fill:#6c63ff,stroke:#3d36cc,color:#fff,font-weight:bold
    classDef core       fill:#1e8bc3,stroke:#145e87,color:#fff
    classDef support    fill:#27ae60,stroke:#1a7a42,color:#fff
    classDef broker     fill:#e74c3c,stroke:#a93226,color:#fff,font-weight:bold
    classDef client     fill:#ecf0f1,stroke:#7f8c8d,color:#000

    class GATEWAY     gateway
    class EUREKA      discovery
    class CUSTOMER,ACCOUNT,TRANSACTION core
    class NOTIFICATION support
    class MQ          broker
    class CLIENT      client
```

