#!/bin/bash
set -e
kubectl delete -f test-daemonset.yml --ignore-not-found
kubectl delete -f test-daemonset-flicker.yml --ignore-not-found
kubectl delete -f test-statefulset.yml --ignore-not-found
kubectl delete -f test-statefulset-flicker.yml --ignore-not-found
kubectl delete -f test-cronjob.yml --ignore-not-found
kubectl delete -f test-cronjob-fail.yml --ignore-not-found
echo "Alle Test-Ressourcen geloescht."
