#!/usr/bin/env bash
# =============================================================================
# teardown-k8s.sh
# Removes all Banking Microservices resources from Kubernetes.
#
# Usage:
#   chmod +x scripts/teardown-k8s.sh
#   ./scripts/teardown-k8s.sh            # Remove deployments (keep PVCs/data)
#   ./scripts/teardown-k8s.sh --purge    # Remove EVERYTHING including data volumes
# =============================================================================

set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info() { echo -e "${GREEN}[INFO]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }

PURGE=false
for arg in "$@"; do [[ "$arg" == "--purge" ]] && PURGE=true; done

if $PURGE; then
  warn "PURGE mode: ALL data (PersistentVolumes) will be deleted!"
  read -rp "Are you sure? This cannot be undone. [yes/N] " CONFIRM
  [[ "$CONFIRM" == "yes" ]] || { info "Aborting."; exit 0; }
  kubectl delete namespace banking --ignore-not-found=true
  info "Namespace banking and ALL resources (including data) deleted."
else
  info "Removing all Deployments and Services (PVCs/data are preserved)..."
  kubectl delete deployments,services,ingresses --all -n banking --ignore-not-found=true
  info "Done. PersistentVolumeClaims are retained. Use --purge to delete data too."
fi

info "Teardown complete."

