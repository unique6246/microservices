#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# start-all.sh — Start the full banking microservices stack with Docker Compose
# ─────────────────────────────────────────────────────────────────────────────
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "[Banking Platform] Checking for .env file..."
if [ ! -f "$ROOT_DIR/.env" ]; then
  echo "ERROR: .env file not found. Run: cp .env.example .env  and fill in secrets."
  exit 1
fi

cd "$ROOT_DIR"

echo "[Banking Platform] Building and starting all services..."
docker-compose up --build -d

echo ""
echo "[Banking Platform] Waiting 30s for services to initialize..."
sleep 30

echo ""
echo "[Banking Platform] Service URLs:"
echo "  Eureka Dashboard    : http://localhost:8761"
echo "  API Gateway         : http://localhost:8080"
echo "  Account Service     : http://localhost:8001/swagger-ui.html"
echo "  Customer Service    : http://localhost:8002/swagger-ui.html"
echo "  Transaction Service : http://localhost:8003/swagger-ui.html"
echo "  Notification Service: http://localhost:8004/swagger-ui.html"
echo "  RabbitMQ Management : http://localhost:15672"
echo "  Zipkin Tracing      : http://localhost:9411"
echo ""
echo "Run 'docker-compose logs -f <service>' to tail logs"

