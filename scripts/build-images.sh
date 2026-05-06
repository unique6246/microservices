#!/usr/bin/env bash
# =============================================================================
# build-images.sh
# Builds all microservice Docker images and pushes them to the local registry
# at localhost:5000.
#
# Usage:
#   chmod +x scripts/build-images.sh
#   ./scripts/build-images.sh
#
# Run from the repository ROOT directory (where docker-compose.yml lives).
# =============================================================================

set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
heading() { echo -e "\n${CYAN}▶ $*${NC}"; }

# ── Auto-detect container runtime (Podman or Docker) ─────────────────────────
if command -v podman &>/dev/null; then
  DOCKER_CMD="podman"
elif command -v docker &>/dev/null; then
  DOCKER_CMD="docker"
else
  echo -e "${RED}[ERROR]${NC} Neither podman nor docker found. Run install-k8s-tools.sh first."
  exit 1
fi
info "Using container runtime: $DOCKER_CMD"

# ── Resolve project root ─────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"
info "Project root: $PROJECT_ROOT"

# ── Registry settings ────────────────────────────────────────────────────────
REGISTRY="${REGISTRY:-localhost:5000}"
TAG="${TAG:-latest}"
NAMESPACE="banking"

info "Registry : $REGISTRY"
info "Tag      : $TAG"
echo ""

# ── Helper: build + tag + push ───────────────────────────────────────────────
build_and_push() {
  local name=$1
  local context=$2
  local dockerfile="${3:-Dockerfile}"
  local image="$REGISTRY/$NAMESPACE/$name:$TAG"

  heading "Building $name"
  info "  Context    : $context"
  info "  Dockerfile : $context/$dockerfile"
  info "  Image      : $image"

  $DOCKER_CMD build \
    --file "$context/$dockerfile" \
    --tag  "$image" \
    "$context"

  info "Pushing $image ..."
  if [ "$DOCKER_CMD" = "podman" ]; then
    $DOCKER_CMD push --tls-verify=false "$image"
  else
    $DOCKER_CMD push "$image"
  fi
  info "✔  $name pushed successfully."
}

# =============================================================================
# Build all services
# =============================================================================
build_and_push "eureka-server"       "./Eureka-server"
build_and_push "account-service"     "./Accountservices"
build_and_push "customer-service"    "./CoustomerServices"
build_and_push "transaction-service" "./TransactionService"
build_and_push "notification-service" "./Notification-Service"
build_and_push "api-gateway"         "./Api-gateway"

# =============================================================================
# Summary
# =============================================================================
echo ""
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo -e "${GREEN} All images built & pushed successfully!${NC}"
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo ""
echo "Images in registry $REGISTRY/$NAMESPACE/:"
$DOCKER_CMD images --format "  {{.Repository}}:{{.Tag}}" | grep "$REGISTRY/$NAMESPACE" || true
echo ""
info "Next step: Run  ./scripts/deploy-k8s.sh  to deploy to Kubernetes."

