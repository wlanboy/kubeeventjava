# Kubernetes Event Dashboard
A lightweight, real‑time dashboard for collecting, storing, searching, and visualizing Kubernetes Events. This project provides:

A FastAPI backend that watches Kubernetes events, stores them in a database, and exposes REST + SSE endpoints
A minimal frontend for live event streaming, searching, filtering, and pagination
Prometheus metrics for monitoring event activity
A clean, self‑contained solution for debugging clusters and understanding workload behavior

Java version of https://github.com/wlanboy/kubeevent

## Start service
```bash
mkdir data
mvn spring-boot:run
```

## Run service in cluster
```bash
POD=$(kubectl get pod -n kubeevent -l app=kubeevent -o jsonpath='{.items[0].metadata.name}')

curl -fsSL https://raw.githubusercontent.com/metalbear-co/mirrord/main/scripts/install.sh | bash

mirrord exec -t pod/$POD -n kubeevent  -- mvn spring-boot:run
```

