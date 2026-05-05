#!/usr/bin/env bash
# =============================================================================
# install-k8s-tools.sh
# Installs: Docker, kubectl, k3s (lightweight Kubernetes), and a local
# Docker registry on the VM.
#
# Tested on: Ubuntu 22.04 / 24.04, Debian 12
# Run as root or with sudo.
# =============================================================================

set -euo pipefail

# ── Colours ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
heading() { echo -e "\n${GREEN}════════════════════════════════════════${NC}"; \
            echo -e "${GREEN} $*${NC}"; \
            echo -e "${GREEN}════════════════════════════════════════${NC}"; }

# ── Root check ───────────────────────────────────────────────────────────────
if [[ $EUID -ne 0 ]]; then
  echo -e "${RED}[ERROR]${NC} Please run as root or with sudo."
  exit 1
fi

# ── Detect OS ────────────────────────────────────────────────────────────────
if [ -f /etc/os-release ]; then
  . /etc/os-release
  OS_ID=$ID
else
  OS_ID="unknown"
fi
info "Detected OS: $OS_ID"

# =============================================================================
# 1. Install Docker
# =============================================================================
heading "Step 1: Installing Docker"

if command -v docker &>/dev/null; then
  info "Docker already installed: $(docker --version)"
else
  info "Installing Docker via official script..."
  curl -fsSL https://get.docker.com | sh

  # Add current non-root user to docker group
  if [ -n "${SUDO_USER:-}" ]; then
    usermod -aG docker "$SUDO_USER"
    info "Added $SUDO_USER to the docker group. Re-login or run: newgrp docker"
  fi

  systemctl enable --now docker
  info "Docker installed: $(docker --version)"
fi

# =============================================================================
# 2. Install kubectl
# =============================================================================
heading "Step 2: Installing kubectl"

if command -v kubectl &>/dev/null; then
  info "kubectl already installed: $(kubectl version --client --short 2>/dev/null || kubectl version --client)"
else
  KUBECTL_VERSION=$(curl -fsSL https://dl.k8s.io/release/stable.txt)
  info "Downloading kubectl $KUBECTL_VERSION..."
  curl -fsSLo /usr/local/bin/kubectl \
    "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl"
  chmod +x /usr/local/bin/kubectl
  info "kubectl installed: $(kubectl version --client --short 2>/dev/null || kubectl version --client)"
fi

# =============================================================================
# 3. Install k3s (lightweight Kubernetes)
# =============================================================================
heading "Step 3: Installing k3s"

if command -v k3s &>/dev/null; then
  info "k3s already installed: $(k3s --version)"
else
  info "Installing k3s (single-node cluster)..."
  # --write-kubeconfig-mode makes the config readable for non-root users
  curl -sfL https://get.k3s.io | \
    INSTALL_K3S_EXEC="--write-kubeconfig-mode 644 \
                      --disable traefik" \
    sh -

  # Wait for k3s to be ready
  info "Waiting for k3s to be ready..."
  sleep 10
  until k3s kubectl get nodes 2>/dev/null | grep -q "Ready"; do
    sleep 5
    echo -n "."
  done
  echo ""
  info "k3s is running!"

  # Set up kubeconfig for non-root user
  mkdir -p /etc/rancher/k3s
  if [ -n "${SUDO_USER:-}" ]; then
    REAL_HOME=$(getent passwd "$SUDO_USER" | cut -d: -f6)
    mkdir -p "$REAL_HOME/.kube"
    cp /etc/rancher/k3s/k3s.yaml "$REAL_HOME/.kube/config"
    chown "$SUDO_USER:$SUDO_USER" "$REAL_HOME/.kube/config"
    chmod 600 "$REAL_HOME/.kube/config"
    info "kubeconfig copied to $REAL_HOME/.kube/config"
  fi

  # Also export KUBECONFIG system-wide
  echo 'export KUBECONFIG=/etc/rancher/k3s/k3s.yaml' >> /etc/profile.d/k3s.sh
  export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

  info "k3s installed: $(k3s --version)"
fi

# =============================================================================
# 4. Install a Local Docker Registry (for storing app images)
# =============================================================================
heading "Step 4: Setting up local Docker registry on port 5000"

if docker ps --format '{{.Names}}' | grep -q "^registry$"; then
  info "Local registry already running."
else
  info "Starting local Docker registry..."
  docker run -d \
    --name registry \
    --restart=always \
    -p 5000:5000 \
    -v /opt/registry-data:/var/lib/registry \
    registry:2

  info "Local registry running at localhost:5000"
fi

# ── Tell k3s to trust the insecure local registry ────────────────────────────
REGISTRIES_FILE="/etc/rancher/k3s/registries.yaml"
if [ ! -f "$REGISTRIES_FILE" ]; then
  info "Configuring k3s to use local registry at localhost:5000..."
  cat > "$REGISTRIES_FILE" <<'EOF'
mirrors:
  "localhost:5000":
    endpoint:
      - "http://localhost:5000"
EOF
  systemctl restart k3s
  info "k3s restarted with local registry config."
else
  info "k3s registries.yaml already exists – skipping."
fi

# =============================================================================
# 5. Install helm (optional but handy)
# =============================================================================
heading "Step 5: Installing Helm"

if command -v helm &>/dev/null; then
  info "Helm already installed: $(helm version --short)"
else
  info "Installing Helm..."
  curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
  info "Helm installed: $(helm version --short)"
fi

# =============================================================================
# Done
# =============================================================================
heading "All tools installed successfully!"
echo ""
echo "  Docker  : $(docker --version)"
echo "  kubectl : $(kubectl version --client --short 2>/dev/null || kubectl version --client)"
echo "  k3s     : $(k3s --version | head -1)"
echo "  Helm    : $(helm version --short)"
echo ""
echo "  Local Registry : http://localhost:5000"
echo "  Kubernetes API : $(kubectl cluster-info 2>/dev/null | head -1)"
echo ""
warn "If you were added to the docker group, log out and back in (or run: newgrp docker)"
echo ""
info "Next step: Run  ./scripts/build-images.sh  to build and push all service images."

