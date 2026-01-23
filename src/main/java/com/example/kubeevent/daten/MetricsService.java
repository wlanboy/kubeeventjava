package com.example.kubeevent.daten;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

        private final MeterRegistry registry;

        public MetricsService(MeterRegistry registry) {
                this.registry = registry;
        }

        public void incrementEventFull(
                        String namespace,
                        String type,
                        String kind,
                        String name,
                        String reason,
                        String component,
                        String host,
                        String deployment) {

                // 1) Gesamt
                Counter.builder("kubeevents_total")
                                .register(registry)
                                .increment();

                // 2) Typ
                Counter.builder("kubeevents_type_total")
                                .tag("type", safe(type))
                                .register(registry)
                                .increment();

                // 3) Namespace
                Counter.builder("kubeevents_namespace_total")
                                .tag("namespace", safe(namespace))
                                .register(registry)
                                .increment();

                // 4) Namespace + Typ
                Counter.builder("kubeevents_namespace_type_total")
                                .tag("namespace", safe(namespace))
                                .tag("type", safe(type))
                                .register(registry)
                                .increment();

                // 5) Involved Object (ohne involved_name wegen Kardinalitäts-Explosion)
                Counter.builder("kubeevents_involved_total")
                                .tag("namespace", safe(namespace))
                                .tag("type", safe(type))
                                .tag("kind", safe(kind))
                                .tag("reason", safe(reason))
                                .tag("component", safe(component))
                                .tag("host", safe(host))
                                .register(registry)
                                .increment();

                // 5b) Reason als dedizierte Metrik für einfaches Alerting
                Counter.builder("kubeevents_reason_total")
                                .tag("namespace", safe(namespace))
                                .tag("reason", safe(reason))
                                .tag("type", safe(type))
                                .register(registry)
                                .increment();

                // 6) Component (nur wenn vorhanden)
                if (component != null && !"unknown".equals(component)) {
                        Counter.builder("kubeevents_component_total")
                                        .tag("component", component)
                                        .register(registry)
                                        .increment();
                }

                // 7) Node/Host (nur wenn vorhanden)
                if (host != null && !"unknown".equals(host)) {
                        Counter.builder("kubeevents_node_total")
                                        .tag("host", host)
                                        .register(registry)
                                        .increment();
                }

                // 8) Deployment (nur wenn vorhanden)
                if (deployment != null && !deployment.isBlank()) {
                        Counter.builder("kubeevents_deployment_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("deployment", deployment)
                                        .tag("type", safe(type))
                                        .register(registry)
                                        .increment();
                }

                // 9) Pod
                if ("Pod".equals(kind)) {
                        Counter.builder("kubeevents_pod_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("pod", safe(name))
                                        .tag("type", safe(type))
                                        .register(registry)
                                        .increment();
                }

                // 10) ReplicaSet
                if ("ReplicaSet".equals(kind)) {
                        Counter.builder("kubeevents_replicaset_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("replicaset", safe(name))
                                        .tag("type", safe(type))
                                        .register(registry)
                                        .increment();
                }

                // 11) StatefulSet
                if ("StatefulSet".equals(kind)) {
                        Counter.builder("kubeevents_statefulset_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("statefulset", safe(name))
                                        .tag("type", safe(type))
                                        .register(registry)
                                        .increment();
                }

                // 12) DaemonSet
                if ("DaemonSet".equals(kind)) {
                        Counter.builder("kubeevents_daemonset_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("daemonset", safe(name))
                                        .tag("type", safe(type))
                                        .register(registry)
                                        .increment();
                }

                // 13) PersistentVolumeClaim (Storage-Probleme)
                if ("PersistentVolumeClaim".equals(kind)) {
                        Counter.builder("kubeevents_pvc_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("pvc", safe(name))
                                        .tag("type", safe(type))
                                        .register(registry)
                                        .increment();
                }

                // 14) Node (Cluster-Health)
                if ("Node".equals(kind)) {
                        Counter.builder("kubeevents_node_events_total")
                                        .tag("node", safe(name))
                                        .tag("type", safe(type))
                                        .tag("reason", safe(reason))
                                        .register(registry)
                                        .increment();
                }
        }

        public void incrementError() {
                Counter.builder("kubeevents_watch_errors_total")
                                .description("Number of watch errors")
                                .register(registry)
                                .increment();
        }

        public void incrementRestart() {
                Counter.builder("kubeevents_watch_restarts_total")
                                .description("Number of watcher restarts")
                                .register(registry)
                                .increment();
        }

        private String safe(String v) {
                return (v == null || v.isBlank()) ? "unknown" : v;
        }
}
