@echo off
:: ─────────────────────────────────────────────────────────────────────────────
:: start-all.bat — Start the full banking microservices stack with Docker Compose
:: ─────────────────────────────────────────────────────────────────────────────

echo [Banking Platform] Checking for .env file...
if not exist .env (
    echo ERROR: .env file not found. Copy .env.example to .env and fill in secrets.
    exit /b 1
)

echo [Banking Platform] Building and starting all services...
docker-compose up --build -d

echo.
echo [Banking Platform] Waiting for services to be healthy...
timeout /t 30 /nobreak > nul

echo.
echo [Banking Platform] Service URLs:
echo   Eureka Dashboard   : http://localhost:8761
echo   API Gateway        : http://localhost:8080
echo   Account Service    : http://localhost:8001/swagger-ui.html
echo   Customer Service   : http://localhost:8002/swagger-ui.html
echo   Transaction Service: http://localhost:8003/swagger-ui.html
echo   Notification Service: http://localhost:8004/swagger-ui.html
echo   RabbitMQ Management: http://localhost:15672
echo   Zipkin Tracing     : http://localhost:9411
echo.
echo Run 'docker-compose logs -f [service-name]' to tail logs

