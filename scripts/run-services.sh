#!/bin/bash
set -e
cd /home/pintu/projects/microservices
set -a
source .env
set +a
echo 'Starting build...'
docker compose up -d --build account-service customer-service transaction-service notification-service api-gateway
echo 'Done'
docker compose ps
