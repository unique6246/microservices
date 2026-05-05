# ─────────────────────────────────────────────────────────────────────────────
# start-local.ps1 — Start the full Banking Microservices stack LOCALLY
#                   (No Docker required)
#
# Usage:
#   cd C:\Users\mkumar27\OT\microservices
#   .\scripts\start-local.ps1
#
# Optional env overrides (set before running):
#   $env:DB_PASS         = "root"        # MySQL root password  (default: root)
#   $env:DB_USER         = "root"        # MySQL user           (default: root)
#   $env:RABBITMQ_USER   = "guest"       # RabbitMQ user        (default: guest)
#   $env:RABBITMQ_PASS   = "guest"       # RabbitMQ password    (default: guest)
#   $env:SMTP_USER       = "..."         # Email for notifications
#   $env:SMTP_PASS       = "..."         # SMTP password / app password
# ─────────────────────────────────────────────────────────────────────────────

$Root = Split-Path -Parent $PSScriptRoot   # repo root

# ─── Helper: launch a service in a new console window ────────────────────────
function Start-Service {
    param(
        [string]$Name,
        [string]$ServiceDir,
        [string]$JarPattern
    )

    $jarFile = Get-ChildItem -Path (Join-Path $Root $ServiceDir "target") `
                             -Filter $JarPattern `
                             -ErrorAction SilentlyContinue |
               Where-Object { $_.Name -notlike "*.original" } |
               Select-Object -First 1

    if (-not $jarFile) {
        Write-Host "  [BUILD] No JAR found for $Name — building with Maven..." -ForegroundColor Yellow
        $mvnw = Join-Path $Root $ServiceDir "mvnw.cmd"
        & $mvnw -f (Join-Path $Root $ServiceDir "pom.xml") clean package -DskipTests -q
        $jarFile = Get-ChildItem -Path (Join-Path $Root $ServiceDir "target") `
                                 -Filter $JarPattern |
                   Where-Object { $_.Name -notlike "*.original" } |
                   Select-Object -First 1
    }

    if (-not $jarFile) {
        Write-Host "  [ERROR] Could not find or build JAR for $Name. Skipping." -ForegroundColor Red
        return
    }

    Write-Host "  [START] $Name  ->  $($jarFile.FullName)" -ForegroundColor Cyan

    # Build java command with any env overrides forwarded as system properties
    $jvmArgs = @()
    if ($env:DB_USER)       { $jvmArgs += "-DDB_USER=$($env:DB_USER)" }
    if ($env:DB_PASS)       { $jvmArgs += "-DDB_PASS=$($env:DB_PASS)" }
    if ($env:RABBITMQ_USER) { $jvmArgs += "-DRABBITMQ_USER=$($env:RABBITMQ_USER)" }
    if ($env:RABBITMQ_PASS) { $jvmArgs += "-DRABBITMQ_PASS=$($env:RABBITMQ_PASS)" }
    if ($env:SMTP_HOST)     { $jvmArgs += "-DSMTP_HOST=$($env:SMTP_HOST)" }
    if ($env:SMTP_PORT)     { $jvmArgs += "-DSMTP_PORT=$($env:SMTP_PORT)" }
    if ($env:SMTP_USER)     { $jvmArgs += "-DSMTP_USER=$($env:SMTP_USER)" }
    if ($env:SMTP_PASS)     { $jvmArgs += "-DSMTP_PASS=$($env:SMTP_PASS)" }

    $jvmArgStr = $jvmArgs -join " "
    $cmd = "java $jvmArgStr -jar `"$($jarFile.FullName)`""

    Start-Process powershell -ArgumentList "-NoExit", "-Command", `
        "Write-Host '=== $Name ===' -ForegroundColor Green; $cmd" `
        -WindowStyle Normal
}

# ─── Banner ──────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "=======================================================" -ForegroundColor Green
Write-Host "  Banking Microservices - Local Start (No Docker)      " -ForegroundColor Green
Write-Host "=======================================================" -ForegroundColor Green
Write-Host ""

# ─── Quick prerequisite checks ───────────────────────────────────────────────
Write-Host "[CHECK] Verifying prerequisites..." -ForegroundColor Yellow

# Java
try {
    $javaVer = & java -version 2>&1 | Select-String "version"
    Write-Host "  Java   : OK  ($javaVer)" -ForegroundColor Green
} catch {
    Write-Host "  [ERROR] Java not found. Install JDK 17+ and add to PATH." -ForegroundColor Red
    exit 1
}

# MySQL
$mysqlRunning = (Test-NetConnection -ComputerName localhost -Port 3306 -WarningAction SilentlyContinue).TcpTestSucceeded
if ($mysqlRunning) {
    Write-Host "  MySQL  : OK  (localhost:3306)" -ForegroundColor Green
} else {
    Write-Host "  [WARN] MySQL is NOT running on localhost:3306. Services will fail to start." -ForegroundColor Red
    Write-Host "         Start MySQL before continuing." -ForegroundColor Red
}

# RabbitMQ
$mqRunning = (Test-NetConnection -ComputerName localhost -Port 5672 -WarningAction SilentlyContinue).TcpTestSucceeded
if ($mqRunning) {
    Write-Host "  RabbitMQ: OK (localhost:5672)" -ForegroundColor Green
} else {
    Write-Host "  [WARN] RabbitMQ is NOT running on localhost:5672. Services will fail to start." -ForegroundColor Red
}

# Redis
$redisRunning = (Test-NetConnection -ComputerName localhost -Port 6379 -WarningAction SilentlyContinue).TcpTestSucceeded
if ($redisRunning) {
    Write-Host "  Redis  : OK  (localhost:6379)" -ForegroundColor Green
} else {
    Write-Host "  [WARN] Redis is NOT running on localhost:6379. Account/Customer caching and Gateway rate-limiting will fail." -ForegroundColor Yellow
}

Write-Host ""

# ─── Start services in order ─────────────────────────────────────────────────
Write-Host "[1/6] Starting Eureka Server (port 8761)..." -ForegroundColor Magenta
Start-Service -Name "Eureka Server" `
              -ServiceDir "Eureka-server" `
              -JarPattern "Eureka-server-*.jar"

Write-Host "      Waiting 20 seconds for Eureka to initialize..." -ForegroundColor DarkGray
Start-Sleep -Seconds 20

Write-Host "[2/6] Starting Account Service (port 8001)..." -ForegroundColor Magenta
Start-Service -Name "Account Service" `
              -ServiceDir "Accountservices" `
              -JarPattern "Accountservices-*.jar"
Start-Sleep -Seconds 5

Write-Host "[3/6] Starting Customer Service (port 8002)..." -ForegroundColor Magenta
Start-Service -Name "Customer Service" `
              -ServiceDir "CoustomerServices" `
              -JarPattern "CoustomerServices-*.jar"
Start-Sleep -Seconds 5

Write-Host "[4/6] Starting Transaction Service (port 8003)..." -ForegroundColor Magenta
Start-Service -Name "Transaction Service" `
              -ServiceDir "TransactionService" `
              -JarPattern "TransactionService-*.jar"
Start-Sleep -Seconds 5

Write-Host "[5/6] Starting Notification Service (port 8004)..." -ForegroundColor Magenta
Start-Service -Name "Notification Service" `
              -ServiceDir "Notification-Service" `
              -JarPattern "Notification-Service-*.jar"
Start-Sleep -Seconds 5

Write-Host "[6/6] Starting API Gateway (port 8080)..." -ForegroundColor Magenta
Start-Service -Name "API Gateway" `
              -ServiceDir "Api-gateway" `
              -JarPattern "Api-gateway-*.jar"

# ─── Done ─────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "=======================================================" -ForegroundColor Green
Write-Host "  All services launched in separate windows.           " -ForegroundColor Green
Write-Host "  Wait ~30 seconds for full registration in Eureka.   " -ForegroundColor Green
Write-Host "=======================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Service URLs:" -ForegroundColor Cyan
Write-Host "  Eureka Dashboard      : http://localhost:8761"
Write-Host "  API Gateway           : http://localhost:8080"
Write-Host "  Account Service       : http://localhost:8001/swagger-ui.html"
Write-Host "  Customer Service      : http://localhost:8002/swagger-ui.html"
Write-Host "  Transaction Service   : http://localhost:8003/swagger-ui.html"
Write-Host "  Notification Service  : http://localhost:8004/swagger-ui.html"
Write-Host ""
Write-Host "To stop all services, close the individual console windows."
Write-Host ""

