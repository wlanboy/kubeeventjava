#!/bin/bash
set -e
kubectl delete -f test-daemonset.yml --ignore-not-found
kubectl delete -f test-daemonset-flicker.yml --ignore-not-found
kubectl delete -f test-statefulset.yml --ignore-not-found
kubectl delete -f test-statefulset-flicker.yml --ignore-not-found
kubectl delete -f test-deployment-flicker.yml --ignore-not-found
kubectl delete -f test-cronjob.yml --ignore-not-found
kubectl delete -f test-cronjob-fail.yml --ignore-not-found
kubectl delete -f test-job-fail.yml --ignore-not-found
kubectl delete -f test-pvc.yml --ignore-not-found
kubectl delete -f test-job-fail-pvc.yml --ignore-not-found
echo "Alle Test-Ressourcen geloescht."
