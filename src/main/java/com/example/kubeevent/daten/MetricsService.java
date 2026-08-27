package com.example.kubeevent.daten;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MetricsService {

        static final String UNKNOWN = "unknown";

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

        public void recordEvent(EventOccurrence event) {
                String safeNamespace = safe(event.namespace());
                String safeType = safe(event.type());
                String safeKind = safe(event.kind());
                String safeName = safe(event.name());
                String safeReason = safe(event.reason());
                String safeHost = safe(event.host());

                recordCoreCounters(safeNamespace, safeType);
                recordInvolvedObjectCounters(event, safeNamespace, safeType, safeKind, safeReason);
                recordOptionalCounters(event, safeNamespace, safeType);
                recordKindSpecificCounter(event.kind(), safeNamespace, safeName, safeType, safeReason);
                recordCriticalReasonCounter(safeReason, safeNamespace, safeKind, safeName, safeHost);
        }

        // Gesamt / Typ / Namespace / Namespace+Typ
        private void recordCoreCounters(String safeNamespace, String safeType) {
                increment(TOTAL);
                increment(TYPE_TOTAL, "type", safeType);
                increment(NAMESPACE_TOTAL, "namespace", safeNamespace);
                increment(NAMESPACE_TYPE_TOTAL, "namespace", safeNamespace, "type", safeType);
        }

        // Involved Object (ohne involved_name wegen Kardinalitäts-Explosion) + dedizierte Reason-Metrik
        private void recordInvolvedObjectCounters(EventOccurrence event, String safeNamespace, String safeType,
                        String safeKind, String safeReason) {
                increment(INVOLVED_TOTAL,
                                "namespace", safeNamespace,
                                "type", safeType,
                                "kind", safeKind,
                                "reason", safeReason,
                                "component", safe(event.component()),
                                "host", safe(event.host()));

                increment(REASON_TOTAL, "namespace", safeNamespace, "reason", safeReason, "type", safeType);
        }

        // Component / Node / Deployment, jeweils nur wenn vorhanden
        private void recordOptionalCounters(EventOccurrence event, String safeNamespace, String safeType) {
                String component = event.component();
                if (component != null && !UNKNOWN.equals(component)) {
                        increment(COMPONENT_TOTAL, "component", component);
                }

                String host = event.host();
                if (host != null && !UNKNOWN.equals(host)) {
                        increment(NODE_TOTAL, "host", host);
                }

                String deployment = event.deployment();
                if (deployment != null && !deployment.isBlank()) {
                        increment(DEPLOYMENT_TOTAL, "namespace", safeNamespace, "deployment", deployment, "type", safeType);
                }
        }

        // Pod / Deployment / ReplicaSet / StatefulSet / DaemonSet / PVC / Node / Job / CronJob / Ingress / PV
        private void recordKindSpecificCounter(String kind, String safeNamespace, String safeName, String safeType,
                        String safeReason) {
                EventKind eventKind = EventKind.fromK8sKind(kind);
                if (eventKind != null) {
                        increment(eventKind.metricName, eventKind.tags(safeNamespace, safeName, safeType, safeReason));
                }
        }

        private void recordCriticalReasonCounter(String safeReason, String safeNamespace, String safeKind,
                        String safeName, String safeHost) {
                CriticalReason criticalReason = CriticalReason.fromReason(safeReason);
                if (criticalReason != null) {
                        increment(criticalReason.metricName,
                                        criticalReason.tags(safeNamespace, safeKind, safeName, safeReason, safeHost));
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
                return (v == null || v.isBlank()) ? UNKNOWN : v;
        }

        // Gibt "kritischen" Reasons (Alerting-relevant) je eine eigene, gezielt getaggte Metrik
        private enum CriticalReason {
                OOM_KILLED("OOMKilled", OOMKILLED_TOTAL, TagShape.NAMESPACE_KIND),
                BACK_OFF("BackOff", BACKOFF_TOTAL, TagShape.NAMESPACE_KIND),
                ERR_IMAGE_PULL("ErrImagePull", IMAGEPULL_ERRORS_TOTAL, TagShape.NAMESPACE_REASON),
                IMAGE_PULL_BACKOFF("ImagePullBackOff", IMAGEPULL_ERRORS_TOTAL, TagShape.NAMESPACE_REASON),
                FAILED_SCHEDULING("FailedScheduling", FAILEDSCHEDULING_TOTAL, TagShape.NAMESPACE_ONLY),
                EVICTED("Evicted", EVICTED_TOTAL, TagShape.NAMESPACE_HOST),
                FAILED_MOUNT("FailedMount", FAILEDMOUNT_TOTAL, TagShape.NAMESPACE_KIND),
                CRASH_LOOP_BACKOFF("CrashLoopBackOff", CRASHLOOPBACKOFF_TOTAL, TagShape.NAMESPACE_KIND),
                UNHEALTHY("Unhealthy", UNHEALTHY_TOTAL, TagShape.NAMESPACE_KIND),
                FAILED("Failed", FAILED_TOTAL, TagShape.NAMESPACE_KIND),
                NODE_NOT_READY("NodeNotReady", NODE_READY_TOTAL, TagShape.NODE_REASON),
                NODE_READY("NodeReady", NODE_READY_TOTAL, TagShape.NODE_REASON),
                FAILED_CREATE("FailedCreate", FAILEDCREATE_TOTAL, TagShape.NAMESPACE_KIND),
                DEADLINE_EXCEEDED("DeadlineExceeded", DEADLINEEXCEEDED_TOTAL, TagShape.NAMESPACE_KIND),
                KILLING("Killing", KILLING_TOTAL, TagShape.NAMESPACE_KIND),
                PREEMPTING("Preempting", PREEMPTING_TOTAL, TagShape.NAMESPACE_KIND);

                private enum TagShape {
                        NAMESPACE_KIND, NAMESPACE_REASON, NAMESPACE_ONLY, NAMESPACE_HOST, NODE_REASON
                }

                private static final Map<String, CriticalReason> BY_REASON = Arrays.stream(values())
                                .collect(Collectors.toMap(r -> r.reason, Function.identity()));

                final String reason;
                final String metricName;
                private final TagShape shape;

                CriticalReason(String reason, String metricName, TagShape shape) {
                        this.reason = reason;
                        this.metricName = metricName;
                        this.shape = shape;
                }

                static CriticalReason fromReason(String reason) {
                        return BY_REASON.get(reason);
                }

                String[] tags(String namespace, String kind, String name, String reason, String host) {
                        return switch (shape) {
                                case NAMESPACE_KIND -> new String[] { "namespace", namespace, "kind", kind };
                                case NAMESPACE_REASON -> new String[] { "namespace", namespace, "reason", reason };
                                case NAMESPACE_ONLY -> new String[] { "namespace", namespace };
                                case NAMESPACE_HOST -> new String[] { "namespace", namespace, "host", host };
                                case NODE_REASON -> new String[] { "node", name, "reason", reason };
                        };
                }
        }
}
