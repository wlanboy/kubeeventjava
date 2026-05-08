#!/bin/bash
set -e
kubectl apply -f test-daemonset.yml
kubectl apply -f test-daemonset-flicker.yml
kubectl apply -f test-statefulset.yml
kubectl apply -f test-statefulset-flicker.yml
kubectl apply -f test-deployment-flicker.yml
kubectl apply -f test-cronjob.yml
kubectl apply -f test-cronjob-fail.yml
kubectl apply -f test-job-fail.yml
kubectl apply -f test-pvc.yml
kubectl apply -f test-job-fail-pvc.yml
echo "Alle Test-Ressourcen angelegt im Namespace kubeeventjava"
echo "Warte auf Pods..."
kubectl rollout status daemonset/test-daemonset -n kubeeventjava --timeout=60s
kubectl rollout status statefulset/test-statefulset -n kubeeventjava --timeout=60s
kubectl rollout status deployment/test-pvc-deployment -n kubeeventjava --timeout=60s
echo "DaemonSet und StatefulSet laufen. CronJob startet jede Minute. Flicker-Ressourcen crashen alle ~10s. PVC gemountet."
