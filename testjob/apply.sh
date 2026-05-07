#!/bin/bash
set -e
kubectl apply -f test-daemonset.yml
kubectl apply -f test-statefulset.yml
kubectl apply -f test-cronjob.yml
kubectl apply -f test-cronjob-fail.yml
echo "Alle Test-Ressourcen angelegt im Namespace kubeeventjava"
echo "Warte auf Pods..."
kubectl rollout status daemonset/test-daemonset -n kubeeventjava --timeout=60s
kubectl rollout status statefulset/test-statefulset -n kubeeventjava --timeout=60s
echo "DaemonSet und StatefulSet laufen. CronJob startet jede Minute."
