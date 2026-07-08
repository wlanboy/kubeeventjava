package com.example.kubeevent.daten;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

enum EventKind {
        POD("Pod", "kubeevents_pod_total", "pod", true, false),
        DEPLOYMENT("Deployment", "kubeevents_deployment_events_total", "deployment", true, true),
        REPLICASET("ReplicaSet", "kubeevents_replicaset_total", "replicaset", true, true),
        STATEFULSET("StatefulSet", "kubeevents_statefulset_total", "statefulset", true, true),
        DAEMONSET("DaemonSet", "kubeevents_daemonset_total", "daemonset", true, true),
        PERSISTENT_VOLUME_CLAIM("PersistentVolumeClaim", "kubeevents_pvc_total", "pvc", true, false),
        NODE("Node", "kubeevents_node_events_total", "node", false, true),
        JOB("Job", "kubeevents_job_total", "job", true, true),
        CRONJOB("CronJob", "kubeevents_cronjob_total", "cronjob", true, true),
        INGRESS("Ingress", "kubeevents_ingress_total", "ingress", true, true),
        PERSISTENT_VOLUME("PersistentVolume", "kubeevents_pv_total", "pv", false, true);

        private static final Map<String, EventKind> BY_K8S_KIND = Arrays.stream(values())
                        .collect(Collectors.toMap(k -> k.k8sKind, Function.identity()));

        final String k8sKind;
        final String metricName;
        final String tagKey;
        final boolean namespaced;
        final boolean includeReason;

        EventKind(String k8sKind, String metricName, String tagKey, boolean namespaced, boolean includeReason) {
                this.k8sKind = k8sKind;
                this.metricName = metricName;
                this.tagKey = tagKey;
                this.namespaced = namespaced;
                this.includeReason = includeReason;
        }

        static EventKind fromK8sKind(String kind) {
                return BY_K8S_KIND.get(kind);
        }

        String[] tags(String safeNamespace, String safeName, String safeType, String safeReason) {
                List<String> tags = new ArrayList<>();
                if (namespaced) {
                        tags.add("namespace");
                        tags.add(safeNamespace);
                }
                tags.add(tagKey);
                tags.add(safeName);
                tags.add("type");
                tags.add(safeType);
                if (includeReason) {
                        tags.add("reason");
                        tags.add(safeReason);
                }
                return tags.toArray(new String[0]);
        }
}
