package com.example.kubeevent.daten;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

        private final MeterRegistry registry;

        public MetricsService(MeterRegistry registry) {
                this.registry = registry;
        }

        @PostConstruct
        public void initializeWatcherMetrics() {
                Counter.builder("kubeevents_watch_errors_total")
                                .description("Number of watch errors")
                                .register(registry);
                Counter.builder("kubeevents_watch_restarts_total")
                                .description("Number of watcher restarts")
                                .register(registry);
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

                // 10) Deployment
                if ("Deployment".equals(kind)) {
                        Counter.builder("kubeevents_deployment_events_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("deployment", safe(name))
                                        .tag("type", safe(type))
                                        .tag("reason", safe(reason))
                                        .register(registry)
                                        .increment();
                }

                // 10b) ReplicaSet
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
                                        .tag("reason", safe(reason))
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

                // 15) Job / CronJob
                if ("Job".equals(kind)) {
                        Counter.builder("kubeevents_job_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("job", safe(name))
                                        .tag("type", safe(type))
                                        .tag("reason", safe(reason))
                                        .register(registry)
                                        .increment();
                }
                if ("CronJob".equals(kind)) {
                        Counter.builder("kubeevents_cronjob_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("cronjob", safe(name))
                                        .tag("type", safe(type))
                                        .tag("reason", safe(reason))
                                        .register(registry)
                                        .increment();
                }

                // 16) Ingress
                if ("Ingress".equals(kind)) {
                        Counter.builder("kubeevents_ingress_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("ingress", safe(name))
                                        .tag("type", safe(type))
                                        .tag("reason", safe(reason))
                                        .register(registry)
                                        .increment();
                }

                // 17) PersistentVolume (cluster-scoped, kein Namespace)
                if ("PersistentVolume".equals(kind)) {
                        Counter.builder("kubeevents_pv_total")
                                        .tag("pv", safe(name))
                                        .tag("type", safe(type))
                                        .tag("reason", safe(reason))
                                        .register(registry)
                                        .increment();
                }

                // 18) Kritische Reasons
                String safeReason = safe(reason);
                if ("OOMKilled".equals(safeReason)) {
                        Counter.builder("kubeevents_oomkilled_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("kind", safe(kind))
                                        .register(registry)
                                        .increment();
                }
                if ("BackOff".equals(safeReason)) {
                        Counter.builder("kubeevents_backoff_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("kind", safe(kind))
                                        .register(registry)
                                        .increment();
                }
                if ("ErrImagePull".equals(safeReason) || "ImagePullBackOff".equals(safeReason)) {
                        Counter.builder("kubeevents_imagepull_errors_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("reason", safeReason)
                                        .register(registry)
                                        .increment();
                }
                if ("FailedScheduling".equals(safeReason)) {
                        Counter.builder("kubeevents_failedscheduling_total")
                                        .tag("namespace", safe(namespace))
                                        .register(registry)
                                        .increment();
                }
                if ("Evicted".equals(safeReason)) {
                        Counter.builder("kubeevents_evicted_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("host", safe(host))
                                        .register(registry)
                                        .increment();
                }
                if ("FailedMount".equals(safeReason)) {
                        Counter.builder("kubeevents_failedmount_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("kind", safe(kind))
                                        .register(registry)
                                        .increment();
                }
                if ("CrashLoopBackOff".equals(safeReason)) {
                        Counter.builder("kubeevents_crashloopbackoff_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("kind", safe(kind))
                                        .register(registry)
                                        .increment();
                }
                if ("Unhealthy".equals(safeReason)) {
                        Counter.builder("kubeevents_unhealthy_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("kind", safe(kind))
                                        .register(registry)
                                        .increment();
                }
                if ("Failed".equals(safeReason)) {
                        Counter.builder("kubeevents_failed_total")
                                        .tag("namespace", safe(namespace))
                                        .tag("kind", safe(kind))
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
