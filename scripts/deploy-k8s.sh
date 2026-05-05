#!/usr/bin/env bash
# =============================================================================
# deploy-k8s.sh
# Deploys the complete Banking Microservices stack to Kubernetes.
#
# Usage:
#   chmod +x scripts/deploy-k8s.sh
#   ./scripts/deploy-k8s.sh          # full deploy
#   ./scripts/deploy-k8s.sh --reset  # tear down and redeploy
#
# Run from the repository ROOT directory.
# =============================================================================

set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()     { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }
heading() { echo -e "\n${CYAN}════════════════════════════════════════${NC}"; \
            echo -e "${CYAN} $*${NC}"; \
            echo -e "${CYAN}════════════════════════════════════════${NC}"; }

# ── Detect project root ───────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
K8S_DIR="$PROJECT_ROOT/k8s"
cd "$PROJECT_ROOT"

# ── kubectl check ─────────────────────────────────────────────────────────────
command -v kubectl &>/dev/null || err "kubectl not found. Run scripts/install-k8s-tools.sh first."
kubectl cluster-info &>/dev/null   || err "Cannot reach Kubernetes cluster. Is k3s/kubectl configured?"

# ── Parse args ────────────────────────────────────────────────────────────────
RESET=false
for arg in "$@"; do
  [[ "$arg" == "--reset" ]] && RESET=true
done

# =============================================================================
# Optional: tear down existing deployment
# =============================================================================
if $RESET; then
  heading "Tearing down existing deployment..."
  kubectl delete namespace banking --ignore-not-found=true
  info "Waiting for namespace to be fully removed..."
  while kubectl get namespace banking &>/dev/null; do sleep 3; echo -n "."; done
  echo ""
  info "Namespace removed."
fi

# =============================================================================
# Step 1 — Namespace + Secrets + ConfigMap
# =============================================================================
heading "Step 1: Applying namespace, secrets, and config"

# Warn user to edit secrets file first
if grep -q "changeme" "$K8S_DIR/01-secrets.yaml" 2>/dev/null; then
  warn "╔══════════════════════════════════════════════════════════╗"
  warn "║  NOTICE: k8s/01-secrets.yaml still has placeholder vals  ║"
  warn "║  Edit it NOW and fill in real passwords before proceeding ║"
  warn "╚══════════════════════════════════════════════════════════╝"
  read -rp "  Have you updated k8s/01-secrets.yaml? [y/N] " CONFIRM
  [[ "${CONFIRM,,}" == "y" ]] || { warn "Aborting. Edit the secrets file first."; exit 0; }
fi

kubectl apply -f "$K8S_DIR/00-namespace.yaml"
kubectl apply -f "$K8S_DIR/01-secrets.yaml"
kubectl apply -f "$K8S_DIR/02-configmap.yaml"
info "Namespace, secrets, and configmap applied."

# =============================================================================
# Step 2 — Infrastructure (databases, brokers, cache, tracing)
# =============================================================================
heading "Step 2: Deploying infrastructure services"

kubectl apply -f "$K8S_DIR/infra/mysql-accounts.yaml"
kubectl apply -f "$K8S_DIR/infra/mysql-customer.yaml"
kubectl apply -f "$K8S_DIR/infra/mysql-transaction.yaml"
kubectl apply -f "$K8S_DIR/infra/mysql-notification.yaml"
kubectl apply -f "$K8S_DIR/infra/rabbitmq.yaml"
kubectl apply -f "$K8S_DIR/infra/redis.yaml"
kubectl apply -f "$K8S_DIR/infra/zipkin.yaml"

info "Waiting for infrastructure pods to become ready (this can take ~2 min)..."
kubectl rollout status deployment/mysql-accounts     -n banking --timeout=180s
kubectl rollout status deployment/mysql-customer     -n banking --timeout=180s
kubectl rollout status deployment/mysql-transaction  -n banking --timeout=180s
kubectl rollout status deployment/mysql-notification -n banking --timeout=180s
kubectl rollout status deployment/rabbitmq           -n banking --timeout=120s
kubectl rollout status deployment/redis              -n banking --timeout=120s
kubectl rollout status deployment/zipkin             -n banking --timeout=60s
info "✔ All infrastructure pods are running."

# =============================================================================
# Step 3 — Eureka Server (service discovery must start before apps)
# =============================================================================
heading "Step 3: Deploying Eureka Server"

kubectl apply -f "$K8S_DIR/apps/eureka-server.yaml"
info "Waiting for Eureka to become ready (up to 3 min)..."
kubectl rollout status deployment/eureka-server -n banking --timeout=180s
info "✔ Eureka Server is running."

# =============================================================================
# Step 4 — Business Services
# =============================================================================
heading "Step 4: Deploying business services"

kubectl apply -f "$K8S_DIR/apps/account-service.yaml"
kubectl apply -f "$K8S_DIR/apps/customer-service.yaml"
kubectl apply -f "$K8S_DIR/apps/transaction-service.yaml"
kubectl apply -f "$K8S_DIR/apps/notification-service.yaml"

info "Waiting for business services to become ready (up to 5 min)..."
kubectl rollout status deployment/account-service      -n banking --timeout=300s
kubectl rollout status deployment/customer-service     -n banking --timeout=300s
kubectl rollout status deployment/transaction-service  -n banking --timeout=300s
kubectl rollout status deployment/notification-service -n banking --timeout=300s
info "✔ All business services are running."

# =============================================================================
# Step 5 — API Gateway + Ingress
# =============================================================================
heading "Step 5: Deploying API Gateway"

kubectl apply -f "$K8S_DIR/apps/api-gateway.yaml"
kubectl apply -f "$K8S_DIR/ingress.yaml"
kubectl rollout status deployment/api-gateway -n banking --timeout=300s
info "✔ API Gateway is running."

# =============================================================================
# Summary
# =============================================================================
heading "Deployment complete!"

VM_IP=$(hostname -I | awk '{print $1}')

echo ""
echo "  ┌─────────────────────────────────────────────────────────────────┐"
echo "  │              Banking Microservices — Access URLs                │"
echo "  ├─────────────────────────────────────────────────────────────────┤"
echo "  │  API Gateway (main entry-point)  http://${VM_IP}:30080          │"
echo "  │  Eureka Dashboard                http://${VM_IP}:30761          │"
echo "  │  RabbitMQ Management UI          http://${VM_IP}:31672          │"
echo "  │  Zipkin Tracing UI               http://${VM_IP}:30411          │"
echo "  └─────────────────────────────────────────────────────────────────┘"
echo ""

info "All pods status:"
kubectl get pods -n banking -o wide
echo ""
info "All services:"
kubectl get services -n banking
echo ""
info "Run  kubectl logs -n banking deploy/<name>  to check any service's logs."

