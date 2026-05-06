#!/usr/bin/env bash
# =============================================================================
# install-k8s-tools.sh
# Installs: kubectl, k3s (lightweight Kubernetes), and a local registry.
# Works with BOTH Docker and Podman (auto-detected).
#
# Tested on: Ubuntu 22.04/24.04, RHEL/Rocky/AlmaLinux 8/9, Debian 12
# Run as root or with sudo:  sudo ./scripts/install-k8s-tools.sh
# =============================================================================

set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()     { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }
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

# ── Detect container runtime (Docker vs Podman) ───────────────────────────────
CONTAINER_CMD=""
if command -v podman &>/dev/null; then
  CONTAINER_CMD="podman"
  info "Detected container runtime: Podman $(podman --version)"
elif command -v docker &>/dev/null; then
  CONTAINER_CMD="docker"
  info "Detected container runtime: Docker $(docker --version)"
fi

# =============================================================================
# 1. Container Runtime (Docker or Podman)
# =============================================================================
heading "Step 1: Container Runtime"

if [ "$CONTAINER_CMD" = "podman" ]; then
  info "Podman is already installed — skipping Docker installation."
  info "Podman is daemonless; no 'docker' group needed."

elif [ "$CONTAINER_CMD" = "docker" ]; then
  info "Docker already installed: $(docker --version)"

else
  # Neither Docker nor Podman — install Docker
  info "No container runtime found. Installing Docker..."
  if [[ "$OS_ID" =~ ^(rhel|centos|rocky|almalinux|fedora)$ ]]; then
    dnf -y install podman podman-docker
    CONTAINER_CMD="podman"
    info "Podman installed (recommended for RHEL-family): $(podman --version)"
  else
    curl -fsSL https://get.docker.com | sh
    CONTAINER_CMD="docker"
    if [ -n "${SUDO_USER:-}" ]; then
      usermod -aG docker "$SUDO_USER"
      info "Added $SUDO_USER to docker group. Re-login or run: newgrp docker"
    fi
    systemctl enable --now docker
    info "Docker installed: $(docker --version)"
  fi
fi

# =============================================================================
# 2. Install kubectl
# =============================================================================
heading "Step 2: Installing kubectl"

if command -v kubectl &>/dev/null; then
  info "kubectl already installed: $(kubectl version --client 2>/dev/null | head -1)"
else
  info "Downloading latest stable kubectl..."
  KUBECTL_VERSION=$(curl -fsSL https://dl.k8s.io/release/stable.txt)
  curl -fsSLo /usr/local/bin/kubectl \
    "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl"
  chmod +x /usr/local/bin/kubectl
  info "kubectl installed: $(kubectl version --client 2>/dev/null | head -1)"
fi

# =============================================================================
# 3. Install k3s (lightweight Kubernetes — uses containerd internally)
# =============================================================================
heading "Step 3: Installing k3s"

if command -v k3s &>/dev/null; then
  info "k3s already installed: $(k3s --version)"
else
  info "Installing k3s single-node cluster..."
  # --write-kubeconfig-mode 644  → non-root users can read kubeconfig
  # --disable traefik            → we manage ingress ourselves
  curl -sfL https://get.k3s.io | \
    INSTALL_K3S_EXEC="--write-kubeconfig-mode 644 --disable traefik" \
    sh -

  info "Waiting for k3s to become ready..."
  sleep 10
  for i in {1..24}; do
    k3s kubectl get nodes 2>/dev/null | grep -q "Ready" && break
    echo -n "."
    sleep 5
  done
  echo ""
  info "k3s is running: $(k3s --version | head -1)"
fi

# ── Set up kubeconfig for the sudo-calling user ───────────────────────────────
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
echo 'export KUBECONFIG=/etc/rancher/k3s/k3s.yaml' > /etc/profile.d/k3s.sh
chmod 644 /etc/profile.d/k3s.sh

if [ -n "${SUDO_USER:-}" ]; then
  REAL_HOME=$(getent passwd "$SUDO_USER" | cut -d: -f6)
  mkdir -p "$REAL_HOME/.kube"
  cp /etc/rancher/k3s/k3s.yaml "$REAL_HOME/.kube/config"
  chown "$SUDO_USER:$SUDO_USER" "$REAL_HOME/.kube/config"
  chmod 600 "$REAL_HOME/.kube/config"
  # Also write KUBECONFIG into user's .bashrc so it persists
  if ! grep -q "KUBECONFIG" "$REAL_HOME/.bashrc" 2>/dev/null; then
    echo 'export KUBECONFIG=$HOME/.kube/config' >> "$REAL_HOME/.bashrc"
  fi
  info "kubeconfig written to $REAL_HOME/.kube/config"
fi

# =============================================================================
# 4. Local Docker/Podman Registry on port 5000
# =============================================================================
heading "Step 4: Setting up local registry on port 5000"

if $CONTAINER_CMD ps --format '{{.Names}}' 2>/dev/null | grep -q "^registry$" || \
   $CONTAINER_CMD ps --format '{{.Names}}' 2>/dev/null | grep -q "registry"; then
  info "Local registry already running."
else
  info "Starting local registry container..."
  mkdir -p /opt/registry-data

  if [ "$CONTAINER_CMD" = "podman" ]; then
    # Podman is daemonless — run registry and create a systemd service for persistence
    podman run -d \
      --name registry \
      -p 5000:5000 \
      -v /opt/registry-data:/var/lib/registry \
      docker.io/library/registry:2

    # Generate systemd unit so registry restarts on reboot
    podman generate systemd --name registry --restart-policy=always \
      > /etc/systemd/system/container-registry.service 2>/dev/null || \
    # Fallback for older Podman versions
    cat > /etc/systemd/system/container-registry.service <<'UNIT'
[Unit]
Description=Podman local registry
After=network.target

[Service]
ExecStart=/usr/bin/podman start -a registry
ExecStop=/usr/bin/podman stop registry
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
UNIT

    systemctl daemon-reload
    systemctl enable container-registry
    info "Registry systemd service created (auto-starts on reboot)."

  else
    # Docker — --restart=always handled by daemon
    docker run -d \
      --name registry \
      --restart=always \
      -p 5000:5000 \
      -v /opt/registry-data:/var/lib/registry \
      registry:2
  fi

  info "Local registry running at http://localhost:5000"
fi

# ── Tell k3s to trust the local insecure registry ────────────────────────────
REGISTRIES_FILE="/etc/rancher/k3s/registries.yaml"
if [ ! -f "$REGISTRIES_FILE" ] || ! grep -q "localhost:5000" "$REGISTRIES_FILE"; then
  info "Configuring k3s to trust local registry at localhost:5000..."
  cat > "$REGISTRIES_FILE" <<'EOF'
mirrors:
  "localhost:5000":
    endpoint:
      - "http://localhost:5000"
EOF
  systemctl restart k3s
  sleep 5
  info "k3s restarted with local registry config."
fi

# =============================================================================
# 5. Install Helm (optional)
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
echo "  Container  : $CONTAINER_CMD $(${CONTAINER_CMD} --version 2>/dev/null | head -1)"
echo "  kubectl    : $(kubectl version --client 2>/dev/null | head -1)"
echo "  k3s        : $(k3s --version | head -1)"
echo "  Helm       : $(helm version --short 2>/dev/null)"
echo ""
echo "  Local Registry : http://localhost:5000"
echo ""
kubectl get nodes
echo ""
warn "IMPORTANT: Run the following to activate kubectl in your current shell:"
echo ""
echo "    export KUBECONFIG=/etc/rancher/k3s/k3s.yaml"
echo ""
echo "  Or log out and back in — it will be set automatically."
echo ""
info "Next step: Run  ./scripts/build-images.sh  to build all service images."
