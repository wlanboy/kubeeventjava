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

        public void incrementEvent(String namespace, String type, String kind, String name, String reason) {

                // 1) Gesamtzahl aller Events
                Counter.builder("kubeevents_total")
                                .description("Total number of Kubernetes events received")
                                .register(registry)
                                .increment();

                // 2) Events nach Typ
                Counter.builder("kubeevents_type_total")
                                .description("Events by type")
                                .tag("type", type)
                                .register(registry)
                                .increment();

                // 3) Events nach Namespace
                Counter.builder("kubeevents_namespace_total")
                                .description("Events by namespace")
                                .tag("namespace", namespace)
                                .register(registry)
                                .increment();

                // 4) Events nach Namespace + Typ
                Counter.builder("kubeevents_namespace_type_total")
                                .description("Events by namespace and type")
                                .tag("namespace", namespace)
                                .tag("type", type)
                                .register(registry)
                                .increment();

                // 5) Events nach Involved Object
                Counter.builder("kubeevents_involved_total")
                                .description("Events by involved object")
                                .tag("namespace", namespace)
                                .tag("type", type)
                                .tag("kind", kind)
                                .tag("involved_name", name)
                                .tag("reason", reason)
                                .tag("component", extractComponent(reason))
                                .register(registry)
                                .increment();

                // 6) Events nach Deployment (falls applicable)
                if ("ReplicaSet".equals(kind) && name.contains("-")) {
                        String deployment = name.substring(0, name.lastIndexOf("-"));
                        Counter.builder("kubeevents_deployment_total")
                                        .description("Events by deployment")
                                        .tag("namespace", namespace)
                                        .tag("deployment", deployment)
                                        .tag("type", type)
                                        .register(registry)
                                        .increment();
                }

                // 7) Events nach Pod
                if ("Pod".equals(kind)) {
                        Counter.builder("kubeevents_pod_total")
                                        .description("Events by pod")
                                        .tag("namespace", namespace)
                                        .tag("pod", name)
                                        .tag("type", type)
                                        .register(registry)
                                        .increment();
                }
        }

        private String extractComponent(String reason) {
                if (reason == null)
                        return "unknown";
                return reason.toLowerCase().contains("kubelet") ? "kubelet" : "controller";
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

        public void incrementEventFull(
                        String namespace,
                        String type,
                        String kind,
                        String name,
                        String reason,
                        String component,
                        String deployment) {
                // 1) Gesamt
                Counter.builder("kubeevents_total").register(registry).increment();

                // 2) Typ
                Counter.builder("kubeevents_type_total")
                                .tag("type", type)
                                .register(registry).increment();

                // 3) Namespace
                Counter.builder("kubeevents_namespace_total")
                                .tag("namespace", namespace)
                                .register(registry).increment();

                // 4) Namespace + Typ
                Counter.builder("kubeevents_namespace_type_total")
                                .tag("namespace", namespace)
                                .tag("type", type)
                                .register(registry).increment();

                // 5) Involved Object
                Counter.builder("kubeevents_involved_total")
                                .tag("namespace", namespace)
                                .tag("type", type)
                                .tag("kind", kind)
                                .tag("involved_name", name)
                                .tag("reason", reason)
                                .tag("component", component)
                                .register(registry).increment();

                // 6) Deployment
                if (deployment != null) {
                        Counter.builder("kubeevents_deployment_total")
                                        .tag("namespace", namespace)
                                        .tag("deployment", deployment)
                                        .tag("type", type)
                                        .register(registry).increment();
                }

                // 7) Pod
                if ("Pod".equals(kind)) {
                        Counter.builder("kubeevents_pod_total")
                                        .tag("namespace", namespace)
                                        .tag("pod", name)
                                        .tag("type", type)
                                        .register(registry).increment();
                }
        }

}
