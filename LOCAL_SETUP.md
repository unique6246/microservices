# 🚀 Running Banking Microservices Locally (No Docker)

## Prerequisites

Install the following before running:

| Tool | Version | Notes |
|------|---------|-------|
| Java (JDK) | 17+ | `java -version` to verify |
| Maven | 3.8+ | Optional — `mvnw` wrapper is included |
| MySQL | 8.0 | Running on `localhost:3306` |
| RabbitMQ | 3.x | Running on `localhost:5672` |
| Redis | 7.x | Running on `localhost:6379` |

> **Zipkin** (tracing) is optional. If not running, services will still work — trace calls will fail silently.

---

## MySQL Setup

Start MySQL and create the four databases (runs once):

```sql
CREATE DATABASE IF NOT EXISTS accounts_db;
CREATE DATABASE IF NOT EXISTS customer_db;
CREATE DATABASE IF NOT EXISTS transaction_db;
CREATE DATABASE IF NOT EXISTS notification_db;
```

Or run from PowerShell (replace `your_password` with your root password):
```powershell
mysql -u root -pyour_password -e "CREATE DATABASE IF NOT EXISTS accounts_db; CREATE DATABASE IF NOT EXISTS customer_db; CREATE DATABASE IF NOT EXISTS transaction_db; CREATE DATABASE IF NOT EXISTS notification_db;"
```

> **Default dev credentials** used by the services: `root` / `root`  
> Override by setting `DB_USER` and `DB_PASS` env variables, or edit `application.yml` dev profile.

---

## RabbitMQ Setup

The services use the default RabbitMQ credentials:
- **User:** `guest`
- **Password:** `guest`
- **Port:** `5672`

If you changed the credentials, set env vars:
```powershell
$env:RABBITMQ_USER = "your_user"
$env:RABBITMQ_PASS = "your_pass"
```

---

## Redis Setup

Redis must run **without a password** locally (default dev config).

> If your Redis requires a password, set: `$env:REDIS_PASS = "your_pass"` and update the `application.yml` redis section for the dev profile.

---

## 🚦 Starting All Services

Run the PowerShell start script from the project root:

```powershell
cd C:\Users\mkumar27\OT\microservices
.\scripts\start-local.ps1
```

This will open a separate terminal window for each service in the correct startup order:

| Order | Service | Port |
|-------|---------|------|
| 1 | Eureka Server (service registry) | 8761 |
| 2 | Account Service | 8001 |
| 3 | Customer Service | 8002 |
| 4 | Transaction Service | 8003 |
| 5 | Notification Service | 8004 |
| 6 | API Gateway | 8080 |

> Wait **~30 seconds** after starting Eureka before using the other services.

---

## Service URLs

| Service | URL |
|---------|-----|
| Eureka Dashboard | http://localhost:8761 |
| API Gateway | http://localhost:8080 |
| Account Service Swagger | http://localhost:8001/swagger-ui.html |
| Customer Service Swagger | http://localhost:8002/swagger-ui.html |
| Transaction Service Swagger | http://localhost:8003/swagger-ui.html |
| Notification Service Swagger | http://localhost:8004/swagger-ui.html |

---

## 🔧 Overriding Configuration

All services use the **`dev` profile** by default. You can override any setting with environment variables before running the script:

```powershell
# Example: use a different MySQL password
$env:DB_PASS = "mypassword"

# Example: custom RabbitMQ credentials
$env:RABBITMQ_USER = "admin"
$env:RABBITMQ_PASS = "secret"

# Example: custom SMTP for Notification Service
$env:SMTP_HOST = "smtp.gmail.com"
$env:SMTP_PORT = "587"
$env:SMTP_USER = "your-email@gmail.com"
$env:SMTP_PASS = "your-app-password"
```

---

## 🛠 Running a Single Service Manually

Navigate to the service folder and use the Maven wrapper:

```powershell
# Build (skip tests)
cd Eureka-server
.\mvnw clean package -DskipTests

# Run directly
.\mvnw spring-boot:run

# Or run the pre-built JAR
java -jar target\Eureka-server-0.0.1-SNAPSHOT.jar
```

---

## Stopping Services

Each service runs in its own console window. Close the windows to stop services, or press `Ctrl+C` in each window.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `java.net.ConnectException` on startup | Ensure MySQL, RabbitMQ, and Redis are running |
| Services not appearing in Eureka | Wait 30–60 seconds for registration to complete |
| `FlywayException` on startup | Check DB credentials and that the database exists |
| `redis.clients.jedis.exceptions.JedisConnectionException` | Ensure Redis is running on port 6379 |
| API Gateway returns 503 | Ensure target service is registered in Eureka |
| Notification emails not sending | Configure `SMTP_USER` and `SMTP_PASS` env variables |

