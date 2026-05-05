# ─────────────────────────────────────────────────────────────────────────────
# start-dev-infra.ps1 — Start Redis, RabbitMQ, and Zipkin via Docker (WSL2)
#
# Run this BEFORE start-local.ps1 to get all infrastructure ready.
#
# Usage:
#   cd C:\Users\mkumar27\OT\microservices
#   .\scripts\start-dev-infra.ps1
#
# To stop:
#   wsl -d Ubuntu-20.04 -- docker rm -f redis-dev rabbitmq-dev zipkin-dev
# ─────────────────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "  Banking Dev Infrastructure — Redis · RabbitMQ · Zipkin" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host ""

# ── Check WSL2 + Docker ────────────────────────────────────────────────────
Write-Host "[CHECK] Verifying WSL2 and Docker..." -ForegroundColor Yellow
$wslOk = (wsl --list --verbose 2>&1) -match "Ubuntu"
if (-not $wslOk) {
    Write-Host "  [ERROR] Ubuntu WSL2 not found. Run: wsl --install -d Ubuntu" -ForegroundColor Red
    exit 1
}
$dockerVer = wsl -d Ubuntu-20.04 -- bash -c "docker --version 2>/dev/null"
if (-not $dockerVer) {
    Write-Host "  [ERROR] Docker not found in WSL2. Install Docker in Ubuntu first." -ForegroundColor Red
    exit 1
}
Write-Host "  WSL2   : OK  (Ubuntu-20.04)" -ForegroundColor Green
Write-Host "  Docker : $dockerVer" -ForegroundColor Green

# ── Start containers ───────────────────────────────────────────────────────
Write-Host ""
Write-Host "[START] Starting infrastructure containers..." -ForegroundColor Magenta

wsl -d Ubuntu-20.04 -- bash -c @"
  docker rm -f redis-dev rabbitmq-dev zipkin-dev 2>/dev/null || true

  docker run -d --name redis-dev \
    -p 6379:6379 \
    redis:7-alpine

  docker run -d --name rabbitmq-dev \
    -p 5672:5672 -p 15672:15672 \
    -e RABBITMQ_DEFAULT_USER=guest \
    -e RABBITMQ_DEFAULT_PASS=guest \
    rabbitmq:3.13-management-alpine

  docker run -d --name zipkin-dev \
    -p 9411:9411 \
    openzipkin/zipkin:3
"@

Write-Host ""
Write-Host "[WAIT] Waiting 10 seconds for services to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# ── Verify from Windows ────────────────────────────────────────────────────
function Test-Port { param([int]$Port, [string]$Name)
    $ok = (Test-NetConnection -ComputerName localhost -Port $Port -WarningAction SilentlyContinue).TcpTestSucceeded
    $color = if ($ok) { "Green" } else { "Yellow" }
    $status = if ($ok) { "RUNNING" } else { "STARTING (wait a few more seconds)" }
    Write-Host "  $Name : $status" -ForegroundColor $color
}

Write-Host "[STATUS]" -ForegroundColor Cyan
Test-Port -Port 6379  -Name "Redis          (6379) "
Test-Port -Port 5672  -Name "RabbitMQ       (5672) "
Test-Port -Port 15672 -Name "RabbitMQ UI    (15672)"
Test-Port -Port 9411  -Name "Zipkin         (9411) "

Write-Host ""
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "  Infrastructure ready! Now run: .\scripts\start-local.ps1" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  RabbitMQ Management UI : http://localhost:15672  (guest / guest)" -ForegroundColor White
Write-Host "  Zipkin Tracing UI      : http://localhost:9411" -ForegroundColor White
Write-Host "  Redis                  : localhost:6379  (no password in dev)" -ForegroundColor White
Write-Host ""
Write-Host "To stop all:  wsl -d Ubuntu-20.04 -- docker rm -f redis-dev rabbitmq-dev zipkin-dev" -ForegroundColor DarkGray
Write-Host ""
