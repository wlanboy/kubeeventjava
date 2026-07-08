package com.example.kubeevent.daten;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

        private static final String WATCH_ERRORS_TOTAL = "kubeevents_watch_errors_total";
        private static final String WATCH_RESTARTS_TOTAL = "kubeevents_watch_restarts_total";
        private static final String TOTAL = "kubeevents_total";
        private static final String TYPE_TOTAL = "kubeevents_type_total";
        private static final String NAMESPACE_TOTAL = "kubeevents_namespace_total";
        private static final String NAMESPACE_TYPE_TOTAL = "kubeevents_namespace_type_total";
        private static final String INVOLVED_TOTAL = "kubeevents_involved_total";
        private static final String REASON_TOTAL = "kubeevents_reason_total";
        private static final String COMPONENT_TOTAL = "kubeevents_component_total";
        private static final String NODE_TOTAL = "kubeevents_node_total";
        private static final String DEPLOYMENT_TOTAL = "kubeevents_deployment_total";
        private static final String OOMKILLED_TOTAL = "kubeevents_oomkilled_total";
        private static final String BACKOFF_TOTAL = "kubeevents_backoff_total";
        private static final String IMAGEPULL_ERRORS_TOTAL = "kubeevents_imagepull_errors_total";
        private static final String FAILEDSCHEDULING_TOTAL = "kubeevents_failedscheduling_total";
        private static final String EVICTED_TOTAL = "kubeevents_evicted_total";
        private static final String FAILEDMOUNT_TOTAL = "kubeevents_failedmount_total";
        private static final String CRASHLOOPBACKOFF_TOTAL = "kubeevents_crashloopbackoff_total";
        private static final String UNHEALTHY_TOTAL = "kubeevents_unhealthy_total";
        private static final String FAILED_TOTAL = "kubeevents_failed_total";
        private static final String NODE_READY_TOTAL = "kubeevents_node_ready_total";
        private static final String FAILEDCREATE_TOTAL = "kubeevents_failedcreate_total";
        private static final String DEADLINEEXCEEDED_TOTAL = "kubeevents_deadlineexceeded_total";
        private static final String KILLING_TOTAL = "kubeevents_killing_total";
        private static final String PREEMPTING_TOTAL = "kubeevents_preempting_total";

        private final MeterRegistry registry;

        public MetricsService(MeterRegistry registry) {
                this.registry = registry;
        }

        @PostConstruct
        public void initializeWatcherMetrics() {
                Counter.builder(WATCH_ERRORS_TOTAL)
                                .description("Number of watch errors")
                                .register(registry);
                Counter.builder(WATCH_RESTARTS_TOTAL)
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

                String safeNamespace = safe(namespace);
                String safeType = safe(type);
                String safeKind = safe(kind);
                String safeName = safe(name);
                String safeReason = safe(reason);
                String safeHost = safe(host);

                // 1) Gesamt
                increment(TOTAL);

                // 2) Typ
                increment(TYPE_TOTAL, "type", safeType);

                // 3) Namespace
                increment(NAMESPACE_TOTAL, "namespace", safeNamespace);

                // 4) Namespace + Typ
                increment(NAMESPACE_TYPE_TOTAL, "namespace", safeNamespace, "type", safeType);

                // 5) Involved Object (ohne involved_name wegen Kardinalitäts-Explosion)
                increment(INVOLVED_TOTAL,
                                "namespace", safeNamespace,
                                "type", safeType,
                                "kind", safeKind,
                                "reason", safeReason,
                                "component", safe(component),
                                "host", safeHost);

                // 5b) Reason als dedizierte Metrik für einfaches Alerting
                increment(REASON_TOTAL, "namespace", safeNamespace, "reason", safeReason, "type", safeType);

                // 6) Component (nur wenn vorhanden)
                if (component != null && !"unknown".equals(component)) {
                        increment(COMPONENT_TOTAL, "component", component);
                }

                // 7) Node/Host (nur wenn vorhanden)
                if (host != null && !"unknown".equals(host)) {
                        increment(NODE_TOTAL, "host", host);
                }

                // 8) Deployment (nur wenn vorhanden)
                if (deployment != null && !deployment.isBlank()) {
                        increment(DEPLOYMENT_TOTAL, "namespace", safeNamespace, "deployment", deployment, "type", safeType);
                }

                // 9-17) Pod / Deployment / ReplicaSet / StatefulSet / DaemonSet / PVC / Node / Job / CronJob / Ingress / PV
                EventKind eventKind = EventKind.fromK8sKind(kind);
                if (eventKind != null) {
                        increment(eventKind.metricName, eventKind.tags(safeNamespace, safeName, safeType, safeReason));
                }

                // 18) Kritische Reasons
                if ("OOMKilled".equals(safeReason)) {
                        increment(OOMKILLED_TOTAL, "namespace", safeNamespace, "kind", safeKind);
                }
                if ("BackOff".equals(safeReason)) {
                        increment(BACKOFF_TOTAL, "namespace", safeNamespace, "kind", safeKind);
                }
                if ("ErrImagePull".equals(safeReason) || "ImagePullBackOff".equals(safeReason)) {
                        increment(IMAGEPULL_ERRORS_TOTAL, "namespace", safeNamespace, "reason", safeReason);
                }
                if ("FailedScheduling".equals(safeReason)) {
                        increment(FAILEDSCHEDULING_TOTAL, "namespace", safeNamespace);
                }
                if ("Evicted".equals(safeReason)) {
                        increment(EVICTED_TOTAL, "namespace", safeNamespace, "host", safeHost);
                }
                if ("FailedMount".equals(safeReason)) {
                        increment(FAILEDMOUNT_TOTAL, "namespace", safeNamespace, "kind", safeKind);
                }
                if ("CrashLoopBackOff".equals(safeReason)) {
                        increment(CRASHLOOPBACKOFF_TOTAL, "namespace", safeNamespace, "kind", safeKind);
                }
                if ("Unhealthy".equals(safeReason)) {
                        increment(UNHEALTHY_TOTAL, "namespace", safeNamespace, "kind", safeKind);
                }
                if ("Failed".equals(safeReason)) {
                        increment(FAILED_TOTAL, "namespace", safeNamespace, "kind", safeKind);
                }
                if ("NodeNotReady".equals(safeReason) || "NodeReady".equals(safeReason)) {
                        increment(NODE_READY_TOTAL, "node", safeName, "reason", safeReason);
                }
                if ("FailedCreate".equals(safeReason)) {
                        increment(FAILEDCREATE_TOTAL, "namespace", safeNamespace, "kind", safeKind);
                }
                if ("DeadlineExceeded".equals(safeReason)) {
                        increment(DEADLINEEXCEEDED_TOTAL, "namespace", safeNamespace, "kind", safeKind);
                }
                if ("Killing".equals(safeReason)) {
                        increment(KILLING_TOTAL, "namespace", safeNamespace, "kind", safeKind);
                }
                if ("Preempting".equals(safeReason)) {
                        increment(PREEMPTING_TOTAL, "namespace", safeNamespace, "kind", safeKind);
                }
        }

        public void incrementError() {
                increment(WATCH_ERRORS_TOTAL);
        }

        public void incrementRestart() {
                increment(WATCH_RESTARTS_TOTAL);
        }

        private void increment(String name, String... tags) {
                registry.counter(name, tags).increment();
        }

        private String safe(String v) {
                return (v == null || v.isBlank()) ? "unknown" : v;
        }
}
