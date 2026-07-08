package com.example.kubeevent.daten;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MetricsServiceTest {

	private SimpleMeterRegistry registry;
	private MetricsService metricsService;

	@BeforeEach
	void setUp() {
		registry = new SimpleMeterRegistry();
		metricsService = new MetricsService(registry);
	}

	private double count(String name, String... tags) {
		var counter = registry.find(name).tags(tags).counter();
		return counter == null ? 0d : counter.count();
	}

	@Test
	void initializeWatcherMetrics_registersWatchCounters() {
		metricsService.initializeWatcherMetrics();

		assertThat(registry.find("kubeevents_watch_errors_total").counter()).isNotNull();
		assertThat(registry.find("kubeevents_watch_restarts_total").counter()).isNotNull();
	}

	@Test
	void incrementError_incrementsWatchErrorsCounter() {
		metricsService.initializeWatcherMetrics();

		metricsService.incrementError();
		metricsService.incrementError();

		assertThat(count("kubeevents_watch_errors_total")).isEqualTo(2);
	}

	@Test
	void incrementRestart_incrementsWatchRestartsCounter() {
		metricsService.initializeWatcherMetrics();

		metricsService.incrementRestart();

		assertThat(count("kubeevents_watch_restarts_total")).isEqualTo(1);
	}

	@Test
	void incrementEventFull_incrementsCoreCounters() {
		metricsService.incrementEventFull(
				"default", "Warning", "Pod", "mypod", "OOMKilled", "kubelet", "node1", "myapp");

		assertThat(count("kubeevents_total")).isEqualTo(1);
		assertThat(count("kubeevents_type_total", "type", "Warning")).isEqualTo(1);
		assertThat(count("kubeevents_namespace_total", "namespace", "default")).isEqualTo(1);
		assertThat(count("kubeevents_namespace_type_total", "namespace", "default", "type", "Warning"))
				.isEqualTo(1);
		assertThat(count("kubeevents_involved_total",
				"namespace", "default",
				"type", "Warning",
				"kind", "Pod",
				"reason", "OOMKilled",
				"component", "kubelet",
				"host", "node1"))
				.isEqualTo(1);
		assertThat(count("kubeevents_reason_total", "namespace", "default", "reason", "OOMKilled", "type", "Warning"))
				.isEqualTo(1);
	}

	@Test
	void incrementEventFull_defaultsMissingValuesToUnknown() {
		metricsService.incrementEventFull(null, "", null, null, null, null, null, null);

		assertThat(count("kubeevents_namespace_total", "namespace", "unknown")).isEqualTo(1);
		assertThat(count("kubeevents_type_total", "type", "unknown")).isEqualTo(1);
		assertThat(count("kubeevents_involved_total",
				"namespace", "unknown",
				"type", "unknown",
				"kind", "unknown",
				"reason", "unknown",
				"component", "unknown",
				"host", "unknown"))
				.isEqualTo(1);
	}

	@Test
	void incrementEventFull_componentIsOnlyRecordedWhenPresentAndKnown() {
		metricsService.incrementEventFull("ns", "Normal", "Pod", "p", "Reason", "kubelet", null, null);
		metricsService.incrementEventFull("ns", "Normal", "Pod", "p", "Reason", null, null, null);
		metricsService.incrementEventFull("ns", "Normal", "Pod", "p", "Reason", "unknown", null, null);

		assertThat(count("kubeevents_component_total", "component", "kubelet")).isEqualTo(1);
		assertThat(registry.find("kubeevents_component_total").tags("component", "unknown").counter()).isNull();
	}

	@Test
	void incrementEventFull_hostIsOnlyRecordedWhenPresentAndKnown() {
		metricsService.incrementEventFull("ns", "Normal", "Pod", "p", "Reason", null, "node1", null);
		metricsService.incrementEventFull("ns", "Normal", "Pod", "p", "Reason", null, null, null);
		metricsService.incrementEventFull("ns", "Normal", "Pod", "p", "Reason", null, "unknown", null);

		assertThat(count("kubeevents_node_total", "host", "node1")).isEqualTo(1);
		assertThat(registry.find("kubeevents_node_total").tags("host", "unknown").counter()).isNull();
	}

	@Test
	void incrementEventFull_deploymentIsOnlyRecordedWhenPresentAndNonBlank() {
		metricsService.incrementEventFull("ns", "Warning", "Deployment", "d", "Reason", null, null, "myapp");
		metricsService.incrementEventFull("ns", "Warning", "Deployment", "d", "Reason", null, null, null);
		metricsService.incrementEventFull("ns", "Warning", "Deployment", "d", "Reason", null, null, "  ");

		assertThat(count("kubeevents_deployment_total", "namespace", "ns", "deployment", "myapp", "type", "Warning"))
				.isEqualTo(1);
	}

	@Test
	void incrementEventFull_podKind_incrementsPodMetricWithoutReasonTag() {
		metricsService.incrementEventFull("ns", "Warning", "Pod", "mypod", "OOMKilled", null, null, null);

		assertThat(count("kubeevents_pod_total", "namespace", "ns", "pod", "mypod", "type", "Warning"))
				.isEqualTo(1);
	}

	@Test
	void incrementEventFull_deploymentKind_incrementsDeploymentEventsMetricWithReasonTag() {
		metricsService.incrementEventFull("ns", "Warning", "Deployment", "mydeploy", "FailedCreate", null, null,
				null);

		assertThat(count("kubeevents_deployment_events_total",
				"namespace", "ns", "deployment", "mydeploy", "type", "Warning", "reason", "FailedCreate"))
				.isEqualTo(1);
	}

	@Test
	void incrementEventFull_unknownKind_doesNotRegisterAnyKindSpecificMetric() {
		metricsService.incrementEventFull("ns", "Warning", "Secret", "s", "Reason", null, null, null);

		assertThat(registry.getMeters()).noneMatch(m -> m.getId().getName().endsWith("_events_total")
				|| m.getId().getName().matches(".*kubeevents_(pod|replicaset|statefulset|daemonset|pvc|job|cronjob|ingress|pv)_total"));
	}

	@ParameterizedTest
	@CsvSource({
			"OOMKilled,           kubeevents_oomkilled_total",
			"BackOff,             kubeevents_backoff_total",
			"FailedMount,         kubeevents_failedmount_total",
			"CrashLoopBackOff,    kubeevents_crashloopbackoff_total",
			"Unhealthy,           kubeevents_unhealthy_total",
			"Failed,              kubeevents_failed_total",
			"FailedCreate,        kubeevents_failedcreate_total",
			"DeadlineExceeded,    kubeevents_deadlineexceeded_total",
			"Killing,             kubeevents_killing_total",
			"Preempting,          kubeevents_preempting_total"
	})
	void incrementEventFull_criticalReason_incrementsDedicatedNamespaceKindCounter(String reason, String metric) {
		metricsService.incrementEventFull("ns", "Warning", "Pod", "p", reason, null, null, null);

		assertThat(count(metric, "namespace", "ns", "kind", "Pod")).isEqualTo(1);
	}

	@ParameterizedTest
	@CsvSource({"ErrImagePull", "ImagePullBackOff"})
	void incrementEventFull_imagePullReasons_incrementImagePullErrorsCounterWithReasonTag(String reason) {
		metricsService.incrementEventFull("ns", "Warning", "Pod", "p", reason, null, null, null);

		assertThat(count("kubeevents_imagepull_errors_total", "namespace", "ns", "reason", reason)).isEqualTo(1);
	}

	@Test
	void incrementEventFull_failedSchedulingReason_incrementsNamespaceOnlyCounter() {
		metricsService.incrementEventFull("ns", "Warning", "Pod", "p", "FailedScheduling", null, null, null);

		assertThat(count("kubeevents_failedscheduling_total", "namespace", "ns")).isEqualTo(1);
	}

	@Test
	void incrementEventFull_evictedReason_incrementsNamespaceAndHostCounter() {
		metricsService.incrementEventFull("ns", "Warning", "Pod", "p", "Evicted", null, "node1", null);

		assertThat(count("kubeevents_evicted_total", "namespace", "ns", "host", "node1")).isEqualTo(1);
	}

	@ParameterizedTest
	@CsvSource({
			"NodeNotReady, NodeNotReady",
			"NodeReady,    NodeReady"
	})
	void incrementEventFull_nodeReasons_incrementNodeReadyCounterWithReasonTag(String reason, String expectedTag) {
		metricsService.incrementEventFull("ns", "Normal", "Node", "node1", reason, null, null, null);

		assertThat(count("kubeevents_node_ready_total", "node", "node1", "reason", expectedTag)).isEqualTo(1);
	}

	@Test
	void incrementEventFull_ordinaryReason_doesNotIncrementAnyCriticalReasonCounter() {
		metricsService.incrementEventFull("ns", "Normal", "Pod", "p", "Scheduled", null, null, null);

		assertThat(registry.getMeters()).noneMatch(m -> m.getId().getName().equals("kubeevents_oomkilled_total")
				|| m.getId().getName().equals("kubeevents_backoff_total")
				|| m.getId().getName().equals("kubeevents_failed_total"));
	}
}
