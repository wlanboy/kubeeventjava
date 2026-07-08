package com.example.kubeevent.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.kubeevent.daten.K8sEvent;
import com.example.kubeevent.daten.K8sEventRepository;
import com.example.kubeevent.daten.MetricsService;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.CoreV1EventSeries;
import io.kubernetes.client.openapi.models.V1EventSource;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;

class K8sWatcherServiceTest {

	private static final String UID = "uid-1";
	private static final String EVENT_NAME = "evt-1";
	private static final String NAMESPACE = "default";
	private static final String TYPE = "Normal";
	private static final String REASON = "Scheduled";
	private static final String MESSAGE = "msg";
	private static final String ACTION = "Add";
	private static final int COUNT = 1;

	private static final String POD_KIND = "Pod";
	private static final String POD_NAME = "mypod";
	private static final String DEPLOYMENT_KIND = "Deployment";
	private static final String DEPLOYMENT_NAME = "myapp";
	private static final String REPLICASET_KIND = "ReplicaSet";
	private static final String REPLICASET_NAME_WITH_HASH = "myapp-7d8f9c6b5";
	private static final String UNKNOWN = "unknown";

	private static final String COMPONENT = "kubelet";
	private static final String REPORTING_COMPONENT = "scheduler";
	private static final String HOST = "node1";

	private K8sEventRepository repository;
	private MetricsService metricsService;
	private K8sWatcherService service;

	@BeforeEach
	void setUp() {
		repository = mock(K8sEventRepository.class);
		metricsService = mock(MetricsService.class);
		WatcherConfig watcherConfig = mock(WatcherConfig.class);
		ApiClient apiClient = mock(ApiClient.class);
		service = new K8sWatcherService(repository, watcherConfig, metricsService, apiClient);
		when(repository.existsByUidAndCount(anyString(), any())).thenReturn(false);
	}

	private static CoreV1Event newEvent() {
		return new CoreV1Event()
				.metadata(new V1ObjectMeta().uid(UID).name(EVENT_NAME).namespace(NAMESPACE))
				.type(TYPE)
				.reason(REASON)
				.message(MESSAGE)
				.involvedObject(new V1ObjectReference().kind(POD_KIND).name(POD_NAME))
				.source(new V1EventSource().component(COMPONENT).host(HOST))
				.count(COUNT)
				.action(ACTION);
	}

	@Test
	void processIncomingEvent_newEvent_savesEntityAndIncrementsMetrics() {
		CoreV1Event event = newEvent();

		service.processIncomingEvent(event);

		ArgumentCaptor<K8sEvent> captor = ArgumentCaptor.forClass(K8sEvent.class);
		verify(repository).save(captor.capture());
		K8sEvent saved = captor.getValue();
		assertThat(saved.getUid()).isEqualTo(UID);
		assertThat(saved.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(saved.getReason()).isEqualTo(REASON);
		assertThat(saved.getInvolvedKind()).isEqualTo(POD_KIND);
		assertThat(saved.getInvolvedName()).isEqualTo(POD_NAME);
		assertThat(saved.getSourceComponent()).isEqualTo(COMPONENT);
		assertThat(saved.getSourceHost()).isEqualTo(HOST);
		assertThat(saved.getCount()).isEqualTo(COUNT);

		verify(metricsService).incrementEventFull(
				NAMESPACE, TYPE, POD_KIND, POD_NAME, REASON, COMPONENT, HOST, null);
		verify(metricsService, never()).incrementError();
	}

	@Test
	void processIncomingEvent_duplicateEvent_doesNotSaveOrIncrementMetrics() {
		when(repository.existsByUidAndCount(UID, COUNT)).thenReturn(true);

		service.processIncomingEvent(newEvent());

		verify(repository, never()).save(any());
		verify(metricsService, never()).incrementEventFull(any(), any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void processIncomingEvent_deploymentKind_derivesDeploymentNameDirectly() {
		CoreV1Event event = newEvent()
				.involvedObject(new V1ObjectReference().kind(DEPLOYMENT_KIND).name(DEPLOYMENT_NAME));

		service.processIncomingEvent(event);

		verify(metricsService).incrementEventFull(
				NAMESPACE, TYPE, DEPLOYMENT_KIND, DEPLOYMENT_NAME, REASON, COMPONENT, HOST, DEPLOYMENT_NAME);
	}

	@Test
	void processIncomingEvent_replicaSetKind_derivesDeploymentNameFromPodHashSuffix() {
		CoreV1Event event = newEvent()
				.involvedObject(new V1ObjectReference().kind(REPLICASET_KIND).name(REPLICASET_NAME_WITH_HASH));

		service.processIncomingEvent(event);

		verify(metricsService).incrementEventFull(
				NAMESPACE, TYPE, REPLICASET_KIND, REPLICASET_NAME_WITH_HASH, REASON, COMPONENT, HOST,
				DEPLOYMENT_NAME);
	}

	@Test
	void processIncomingEvent_replicaSetKind_withoutDashInName_leavesDeploymentNull() {
		CoreV1Event event = newEvent()
				.involvedObject(new V1ObjectReference().kind(REPLICASET_KIND).name(DEPLOYMENT_NAME));

		service.processIncomingEvent(event);

		verify(metricsService).incrementEventFull(
				NAMESPACE, TYPE, REPLICASET_KIND, DEPLOYMENT_NAME, REASON, COMPONENT, HOST, null);
	}

	@Test
	void processIncomingEvent_involvedObjectMissing_defaultsKindAndNameToUnknown() {
		CoreV1Event event = newEvent().involvedObject(null);

		service.processIncomingEvent(event);

		verify(metricsService).incrementEventFull(
				NAMESPACE, TYPE, UNKNOWN, UNKNOWN, REASON, COMPONENT, HOST, null);
	}

	@Test
	void processIncomingEvent_componentFallsBackToReportingComponentWhenSourceMissing() {
		CoreV1Event event = newEvent().source(null).reportingComponent(REPORTING_COMPONENT);

		service.processIncomingEvent(event);

		verify(metricsService).incrementEventFull(
				NAMESPACE, TYPE, POD_KIND, POD_NAME, REASON, REPORTING_COMPONENT, UNKNOWN, null);
	}

	@Test
	void processIncomingEvent_componentAndHostDefaultToUnknownWhenNothingReported() {
		CoreV1Event event = newEvent().source(null).reportingComponent(null);

		service.processIncomingEvent(event);

		verify(metricsService).incrementEventFull(
				NAMESPACE, TYPE, POD_KIND, POD_NAME, REASON, UNKNOWN, UNKNOWN, null);
	}

	@Test
	void processIncomingEvent_firstTimestampFallsBackToEventTimeWhenMissing() {
		OffsetDateTime eventTime = OffsetDateTime.parse("2026-07-08T10:00:00Z");
		CoreV1Event event = newEvent().firstTimestamp(null).eventTime(eventTime);

		service.processIncomingEvent(event);

		ArgumentCaptor<K8sEvent> captor = ArgumentCaptor.forClass(K8sEvent.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getFirstTimestamp()).isEqualTo(eventTime);
	}

	@Test
	void processIncomingEvent_firstTimestampIsUsedWhenPresent() {
		OffsetDateTime firstTimestamp = OffsetDateTime.parse("2026-07-08T09:00:00Z");
		OffsetDateTime eventTime = OffsetDateTime.parse("2026-07-08T10:00:00Z");
		CoreV1Event event = newEvent().firstTimestamp(firstTimestamp).eventTime(eventTime);

		service.processIncomingEvent(event);

		ArgumentCaptor<K8sEvent> captor = ArgumentCaptor.forClass(K8sEvent.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getFirstTimestamp()).isEqualTo(firstTimestamp);
	}

	@ParameterizedTest
	@CsvSource({
			"5,    ,    5",
			",     3,   3",
			",     ,    1"
	})
	void processIncomingEvent_countFallsBackToSeriesThenToOne(Integer count, Integer seriesCount, int expected) {
		CoreV1Event event = newEvent().count(count);
		if (seriesCount != null) {
			event.series(new CoreV1EventSeries().count(seriesCount));
		}

		service.processIncomingEvent(event);

		verify(repository).existsByUidAndCount(UID, expected);
		ArgumentCaptor<K8sEvent> captor = ArgumentCaptor.forClass(K8sEvent.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getCount()).isEqualTo(expected);
	}

	@Test
	void processIncomingEvent_dataIntegrityViolation_isSwallowedWithoutIncrementingError() {
		when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate uid+count"));

		service.processIncomingEvent(newEvent());

		verify(metricsService, never()).incrementError();
	}

	@Test
	void processIncomingEvent_unexpectedException_incrementsErrorMetric() {
		when(repository.save(any())).thenThrow(new RuntimeException("db down"));

		service.processIncomingEvent(newEvent());

		verify(metricsService, times(1)).incrementError();
	}
}
