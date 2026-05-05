# Kubernetes Deployment Guide — Banking Microservices

> **Target:** Single Linux VM (Ubuntu 22.04 / 24.04 recommended)  
> **Kubernetes distribution:** [k3s](https://k3s.io/) — lightweight, production-ready  
> **Min VM specs:** 4 vCPU · 8 GB RAM · 40 GB disk

---

## Architecture Overview

```
External Traffic
      │
      ▼ :30080 (NodePort)
 ┌──────────┐
 │API Gateway│ ← Redis (rate-limit/cache)
 └────┬─────┘
      │ routes via Eureka service discovery
      ├──→ Account Service    :8001  ← MySQL accounts_db
      ├──→ Customer Service   :8002  ← MySQL customer_db
      ├──→ Transaction Service:8003  ← MySQL transaction_db
      └──→ Notification Svc  :8004  ← MySQL notification_db
                                     ← RabbitMQ (async events)
                                     ← SMTP (email)

Supporting: Eureka Server :8761  |  Zipkin :9411  |  RabbitMQ UI :15672
```

---

## Step-by-Step Setup

### Prerequisites on the Linux VM

```bash
# Log into your Linux VM via SSH
ssh user@<VM_IP>

# Clone / copy your project to the VM
git clone <your-repo-url>   # OR use scp / rsync
cd microservices
```

---

### Step 1 — Install Tools (Docker, kubectl, k3s, Helm)

```bash
chmod +x scripts/install-k8s-tools.sh
sudo ./scripts/install-k8s-tools.sh
```

This script will install:
| Tool | Purpose |
|------|---------|
| **Docker** | Build and run container images |
| **kubectl** | Kubernetes CLI |
| **k3s** | Lightweight single-node Kubernetes cluster |
| **Local Registry** | `localhost:5000` — stores your built images |
| **Helm** | Kubernetes package manager (optional) |

After it finishes:
```bash
# Verify everything is running
kubectl get nodes          # should show Ready
docker ps                  # should show registry container
```

---

### Step 2 — Configure Secrets

**Edit `k8s/01-secrets.yaml` and fill in real values:**

```bash
nano k8s/01-secrets.yaml
```

| Key | Description |
|-----|-------------|
| `DB_PASS` | MySQL root password (same for all 4 DBs) |
| `RABBITMQ_USER` / `RABBITMQ_PASS` | RabbitMQ credentials |
| `REDIS_PASS` | Redis password |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USER` / `SMTP_PASS` | Email config for notifications |
| `JWT_ISSUER_URI` | JWT issuer (leave empty `""` to skip JWT validation) |

> ⚠️ **Never commit `01-secrets.yaml` with real values to version control.**  
> Add it to `.gitignore` or use [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets).

---

### Step 3 — Build & Push Docker Images

```bash
chmod +x scripts/build-images.sh
./scripts/build-images.sh
```

This builds each service's Docker image using its `Dockerfile` and pushes to your local registry at `localhost:5000`.

To verify:
```bash
curl http://localhost:5000/v2/banking/eureka-server/tags/list
curl http://localhost:5000/v2/_catalog
```

---

### Step 4 — Deploy to Kubernetes

```bash
chmod +x scripts/deploy-k8s.sh
./scripts/deploy-k8s.sh
```

The script deploys in the correct dependency order:
1. Namespace + Secrets + ConfigMap
2. MySQL × 4 + RabbitMQ + Redis + Zipkin
3. Eureka Server
4. Account, Customer, Transaction, Notification services
5. API Gateway + Ingress

---

### Step 5 — Verify Deployment

```bash
# Check all pods are Running
kubectl get pods -n banking

# Check services & NodePorts
kubectl get services -n banking

# Watch pods in real-time
kubectl get pods -n banking -w
```

Expected output (all pods should be `Running`):
```
NAME                                   READY   STATUS    AGE
mysql-accounts-xxxxxxx                 1/1     Running   5m
mysql-customer-xxxxxxx                 1/1     Running   5m
mysql-transaction-xxxxxxx              1/1     Running   5m
mysql-notification-xxxxxxx             1/1     Running   5m
rabbitmq-xxxxxxx                       1/1     Running   5m
redis-xxxxxxx                          1/1     Running   5m
zipkin-xxxxxxx                         1/1     Running   5m
eureka-server-xxxxxxx                  1/1     Running   4m
account-service-xxxxxxx                1/1     Running   3m
customer-service-xxxxxxx               1/1     Running   3m
transaction-service-xxxxxxx            1/1     Running   3m
notification-service-xxxxxxx           1/1     Running   3m
api-gateway-xxxxxxx                    1/1     Running   2m
```

---

## Access URLs

Replace `<VM_IP>` with your Linux VM's IP address:

| Service | URL | Notes |
|---------|-----|-------|
| **API Gateway** | `http://<VM_IP>:30080` | Main entry-point for all APIs |
| **Eureka Dashboard** | `http://<VM_IP>:30761` | Service registry UI |
| **RabbitMQ Management** | `http://<VM_IP>:31672` | Credentials: your RABBITMQ_USER/PASS |
| **Zipkin Tracing** | `http://<VM_IP>:30411` | Distributed traces |

---

## API Endpoints (via Gateway on port 30080)

```
POST   http://<VM_IP>:30080/customers
GET    http://<VM_IP>:30080/customers/{id}
GET    http://<VM_IP>:30080/customers
DELETE http://<VM_IP>:30080/customers/{id}

POST   http://<VM_IP>:30080/accounts
GET    http://<VM_IP>:30080/accounts/{id}
GET    http://<VM_IP>:30080/accounts/customer/{id}
PUT    http://<VM_IP>:30080/accounts/update
DELETE http://<VM_IP>:30080/accounts/{no}

POST   http://<VM_IP>:30080/transactions/credit
POST   http://<VM_IP>:30080/transactions/debit
POST   http://<VM_IP>:30080/transactions/transfer
GET    http://<VM_IP>:30080/transactions
GET    http://<VM_IP>:30080/transactions/{accountNo}

POST   http://<VM_IP>:30080/notifications/send
```

---

## Useful kubectl Commands

```bash
# View logs for a specific service
kubectl logs -n banking deploy/account-service --follow
kubectl logs -n banking deploy/api-gateway --follow
kubectl logs -n banking deploy/eureka-server --follow

# Describe a pod for debugging
kubectl describe pod -n banking <pod-name>

# Get events (for troubleshooting startup issues)
kubectl get events -n banking --sort-by='.lastTimestamp'

# Execute a shell inside a pod
kubectl exec -it -n banking deploy/rabbitmq -- sh
kubectl exec -it -n banking deploy/redis -- redis-cli -a <REDIS_PASS>

# Scale a service
kubectl scale deployment account-service -n banking --replicas=2

# Rolling restart (to pick up new image)
kubectl rollout restart deployment/account-service -n banking

# Port-forward a service to your local machine
kubectl port-forward -n banking svc/api-gateway 8080:8080
```

---

## Updating Images

When you push new code, rebuild and re-push the image, then restart the deployment:

```bash
# Rebuild specific service
cd microservices
docker build -t localhost:5000/banking/account-service:latest ./Accountservices
docker push localhost:5000/banking/account-service:latest

# Apply the rolling restart
kubectl rollout restart deployment/account-service -n banking
kubectl rollout status deployment/account-service -n banking
```

Or rebuild all services at once:
```bash
./scripts/build-images.sh
kubectl rollout restart deployment -n banking
```

---

## Teardown

```bash
# Remove deployments (keep database data)
chmod +x scripts/teardown-k8s.sh
./scripts/teardown-k8s.sh

# Remove EVERYTHING including databases
./scripts/teardown-k8s.sh --purge

# Or reset and redeploy in one command
./scripts/deploy-k8s.sh --reset
```

---

## Troubleshooting

### Pod stuck in `Pending`
```bash
kubectl describe pod -n banking <pod-name>
# Look for "Events:" section — usually a resource or PVC issue
kubectl get pvc -n banking   # check if PVCs are Bound
```

### Pod stuck in `ImagePullBackOff`
```bash
# Verify the image was pushed
curl http://localhost:5000/v2/banking/<service>/tags/list

# Check k3s registry config
cat /etc/rancher/k3s/registries.yaml

# Restart k3s and try again
sudo systemctl restart k3s
kubectl rollout restart deployment/<service-name> -n banking
```

### Service not registering with Eureka
```bash
# Check Eureka is healthy first
kubectl logs -n banking deploy/eureka-server | tail -50
# Then check the service logs
kubectl logs -n banking deploy/account-service | grep -i eureka
```

### MySQL initialization takes too long
```bash
# Check MySQL pod logs
kubectl logs -n banking deploy/mysql-accounts
# The initContainers wait for port 3306 to open — this is expected
kubectl get pods -n banking   # mysql pod should show Running, then app pod starts
```

### Out of Memory (OOMKilled)
Increase the resource limits in the relevant YAML file:
```yaml
resources:
  limits:
    memory: "1Gi"   # increase from 512Mi
```
Then: `kubectl apply -f k8s/apps/<service>.yaml`

---

## File Structure

```
k8s/
├── 00-namespace.yaml          # banking namespace
├── 01-secrets.yaml            # 🔐 Fill in before deploying
├── 02-configmap.yaml          # Non-sensitive config (URLs, DB names)
├── kustomization.yaml         # Apply everything: kubectl apply -k k8s/
├── ingress.yaml               # HTTP routing
├── infra/
│   ├── mysql-accounts.yaml    # MySQL + PVC (accounts_db)
│   ├── mysql-customer.yaml    # MySQL + PVC (customer_db)
│   ├── mysql-transaction.yaml # MySQL + PVC (transaction_db)
│   ├── mysql-notification.yaml# MySQL + PVC (notification_db)
│   ├── rabbitmq.yaml          # RabbitMQ + PVC + Management NodePort
│   ├── redis.yaml             # Redis + PVC
│   └── zipkin.yaml            # Zipkin + NodePort
└── apps/
    ├── eureka-server.yaml     # Eureka + NodePort :30761
    ├── account-service.yaml   # Account Service
    ├── customer-service.yaml  # Customer Service
    ├── transaction-service.yaml
    ├── notification-service.yaml
    └── api-gateway.yaml       # API Gateway + NodePort :30080

scripts/
├── install-k8s-tools.sh      # Install Docker, kubectl, k3s, registry
├── build-images.sh           # Build + push all Docker images
├── deploy-k8s.sh             # Full deployment in correct order
└── teardown-k8s.sh           # Remove deployment
```

---

## NodePort Reference

| NodePort | Service | Internal Port |
|----------|---------|---------------|
| 30080 | API Gateway | 8080 |
| 30761 | Eureka Server | 8761 |
| 31672 | RabbitMQ Management | 15672 |
| 30411 | Zipkin | 9411 |

