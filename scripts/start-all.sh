#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# start-all.sh — Start the full banking stack with Docker Compose (local dev)
# Usage: ./scripts/start-all.sh
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

echo "[Banking] Checking for .env file..."
if [ ! -f ".env" ]; then
  if [ -f ".env.example" ]; then
    cp .env.example .env
    echo "[Banking] Created .env from .env.example — please review and update secrets."
  else
    echo "ERROR: .env file not found. Create one before continuing."
    exit 1
  fi
fi

echo "[Banking] Starting infrastructure (MySQL, RabbitMQ, Redis, Zipkin)..."
docker-compose -f docker-compose-infra.yml up -d

echo "[Banking] Waiting 15s for infrastructure to initialize..."
sleep 15

echo "[Banking] Building and starting application services..."
docker-compose up --build -d

echo "[Banking] Waiting 30s for services to start..."
sleep 30

echo ""
echo "════════════════════════════════════════════════════════"
echo "  Banking Microservices — Service URLs"
echo "════════════════════════════════════════════════════════"
echo "  Eureka Dashboard    : http://localhost:8761"
echo "  API Gateway         : http://localhost:8080"
echo "  Account Service     : http://localhost:8001/swagger-ui.html"
echo "  Customer Service    : http://localhost:8002/swagger-ui.html"
echo "  Transaction Service : http://localhost:8003/swagger-ui.html"
echo "  Notification Service: http://localhost:8004/swagger-ui.html"
echo "  RabbitMQ Management : http://localhost:15672  (guest/guest)"
echo "  Zipkin Tracing      : http://localhost:9411"
echo "════════════════════════════════════════════════════════"
echo ""
echo "  Get a JWT token:  POST http://localhost:8080/auth/token"
echo '  Body: {"username":"user","roles":["USER","ADMIN"]}'
echo ""
echo "  Tail logs: docker-compose logs -f <service-name>"
echo "  Stop all:  docker-compose down"
echo ""
