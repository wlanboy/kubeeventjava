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
                                .tag("type", type)
                                .register(registry)
                                .increment();

                // 3) Namespace
                Counter.builder("kubeevents_namespace_total")
                                .tag("namespace", namespace)
                                .register(registry)
                                .increment();

                // 4) Namespace + Typ
                Counter.builder("kubeevents_namespace_type_total")
                                .tag("namespace", namespace)
                                .tag("type", type)
                                .register(registry)
                                .increment();

                // 5) Involved Object
                Counter.builder("kubeevents_involved_total")
                                .tag("namespace", namespace)
                                .tag("type", type)
                                .tag("kind", kind)
                                .tag("involved_name", name)
                                .tag("reason", reason)
                                .tag("component", component)
                                .register(registry)
                                .increment();

                // 6) Component (NEU)
                Counter.builder("kubeevents_component_total")
                                .tag("component", component)
                                .register(registry)
                                .increment();

                // 7) Node/Host (NEU)
                Counter.builder("kubeevents_node_total")
                                .tag("host", host)
                                .register(registry)
                                .increment();

                // 8) Deployment
                if (deployment != null) {
                        Counter.builder("kubeevents_deployment_total")
                                        .tag("namespace", namespace)
                                        .tag("deployment", deployment)
                                        .tag("type", type)
                                        .register(registry)
                                        .increment();
                }

                // 9) Pod
                if ("Pod".equals(kind)) {
                        Counter.builder("kubeevents_pod_total")
                                        .tag("namespace", namespace)
                                        .tag("pod", name)
                                        .tag("type", type)
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

}
