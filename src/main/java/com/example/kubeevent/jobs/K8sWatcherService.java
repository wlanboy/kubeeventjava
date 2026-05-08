package com.example.kubeevent.jobs;

import com.example.kubeevent.daten.K8sEvent;
import com.example.kubeevent.daten.K8sEventRepository;
import com.example.kubeevent.daten.MetricsService;

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.CoreV1EventList;
import io.kubernetes.client.util.generic.GenericKubernetesApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@Slf4j
@RequiredArgsConstructor
public class K8sWatcherService {

    private final K8sEventRepository repository;
    private final WatcherConfig watcherConfig;
    private final MetricsService metricsService;
    private final ApiClient apiClient;

    private SharedInformerFactory factory;
    private GenericKubernetesApi<CoreV1Event, CoreV1EventList> eventApi;

    private final Map<String, SharedIndexInformer<CoreV1Event>> informers = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> informerFutures = new ConcurrentHashMap<>();
    private final ExecutorService informerExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "k8s-informer");
        t.setDaemon(true);
        return t;
    });

    private void setupApi() {
        this.factory = new SharedInformerFactory(apiClient);
        this.eventApi = new GenericKubernetesApi<>(
                CoreV1Event.class,
                CoreV1EventList.class,
                "",
                "v1",
                "events",
                apiClient);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[WATCH] Application ready. Initializing Watchers...");
        setupApi();

        List<String> namespaces = watcherConfig.getWatchedNamespaces();
        if (namespaces == null || namespaces.isEmpty()) {
            log.warn("[WATCH] No namespaces configured to watch!");
            return;
        }

        for (String ns : namespaces) {
            String targetNs = ns.trim();
            log.info("[WATCH] Setting up watcher for namespace: {}", targetNs);
            importExistingEvents(targetNs);
            startInformer(targetNs);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("[WATCH] Shutting down informers...");
        informers.values().forEach(i -> { try { i.stop(); } catch (Exception ignored) {} });
        informerExecutor.shutdown();
    }

    // ---------------------------------------------------------
    // HEALTH CHECK + RESTART
    // ---------------------------------------------------------
    private static final long HEALTH_CHECK_DELAY_MS = 30_000L;
    private static final long HEALTH_CHECK_INITIAL_DELAY_MS = 60_000L;

    @Scheduled(fixedDelay = HEALTH_CHECK_DELAY_MS, initialDelay = HEALTH_CHECK_INITIAL_DELAY_MS)
    public void checkWatcherHealth() {
        for (Map.Entry<String, Future<?>> entry : informerFutures.entrySet()) {
            String ns = entry.getKey();
            if (entry.getValue().isDone()) {
                log.warn("[WATCH] Informer for namespace '{}' stopped — restarting", ns);
                metricsService.incrementRestart();
                restartInformerFor(ns);
            }
        }
    }

    private void restartInformerFor(String namespace) {
        try {
            SharedIndexInformer<CoreV1Event> old = informers.get(namespace);
            if (old != null) {
                try { old.stop(); } catch (Exception ignored) {}
            }

            SharedInformerFactory nsFactory = new SharedInformerFactory(apiClient);
            GenericKubernetesApi<CoreV1Event, CoreV1EventList> nsApi = new GenericKubernetesApi<>(
                    CoreV1Event.class, CoreV1EventList.class, "", "v1", "events", apiClient);
            SharedIndexInformer<CoreV1Event> newInformer = nsFactory.sharedIndexInformerFor(
                    nsApi, CoreV1Event.class, RESYNC_PERIOD_MS, namespace);
            addHandlerToInformer(newInformer);
            informers.put(namespace, newInformer);
            informerFutures.put(namespace, informerExecutor.submit(newInformer::run));
            log.info("[WATCH] Informer for namespace '{}' restarted successfully", namespace);
        } catch (Exception e) {
            log.error("[WATCH] Restart of informer for namespace '{}' failed", namespace, e);
            metricsService.incrementError();
        }
    }

    // ---------------------------------------------------------
    // EXISTING EVENTS IMPORT
    // ---------------------------------------------------------
    private void importExistingEvents(String namespace) {
        try {
            var response = eventApi.list(namespace);
            if (!response.isSuccess()) {
                log.error("[WATCH] Failed to list events for '{}': {} - {}",
                        namespace, response.getHttpStatusCode(), response.getStatus());
                metricsService.incrementError();
                return;
            }

            CoreV1EventList list = response.getObject();
            if (list == null || list.getItems() == null) {
                return;
            }

            log.info("[WATCH] Importing {} existing events from namespace '{}'",
                    list.getItems().size(), namespace);

            for (CoreV1Event event : list.getItems()) {
                processIncomingEvent(event);
            }

        } catch (Exception e) {
            log.error("[WATCH] Failed to import existing events for '{}'", namespace, e);
            metricsService.incrementError();
        }
    }

    // ---------------------------------------------------------
    // LIVE WATCHER
    // ---------------------------------------------------------
    private static final long RESYNC_PERIOD_MS = 3600000L;

    private void startInformer(String namespace) {
        SharedIndexInformer<CoreV1Event> informer = factory.sharedIndexInformerFor(
                eventApi,
                CoreV1Event.class,
                RESYNC_PERIOD_MS,
                namespace);
        addHandlerToInformer(informer);
        informers.put(namespace, informer);
        informerFutures.put(namespace, informerExecutor.submit(informer::run));
    }

    private void addHandlerToInformer(SharedIndexInformer<CoreV1Event> informer) {
        informer.addEventHandler(new ResourceEventHandler<CoreV1Event>() {
            @Override
            public void onAdd(CoreV1Event obj) {
                log.debug("[WATCH] onAdd: {} - {}", obj.getReason(), obj.getMessage());
                processIncomingEvent(obj);
            }

            @Override
            public void onUpdate(CoreV1Event oldObj, CoreV1Event newObj) {
                Integer oldCount = safeCount(oldObj);
                Integer newCount = safeCount(newObj);

                if (!oldCount.equals(newCount)) {
                    log.debug("[WATCH] onUpdate: {} ({} → {})",
                            newObj.getReason(), oldCount, newCount);
                    processIncomingEvent(newObj);
                }
            }

            @Override
            public void onDelete(CoreV1Event obj, boolean deletedFinalStateUnknown) {
                log.debug("[WATCH] onDelete: {}", obj.getMetadata().getName());
            }
        });
    }

    // ---------------------------------------------------------
    // EVENT PROCESSING
    // ---------------------------------------------------------
    @Transactional
    protected void processIncomingEvent(CoreV1Event rawEvent) {
        try {
            String uid = rawEvent.getMetadata().getUid();
            Integer count = safeCount(rawEvent);

            if (repository.existsByUidAndCount(uid, count)) {
                return;
            }

            String namespace = rawEvent.getMetadata().getNamespace();
            String type = rawEvent.getType();
            String reason = rawEvent.getReason();
            String message = rawEvent.getMessage();

            String kind = rawEvent.getInvolvedObject() != null
                    ? rawEvent.getInvolvedObject().getKind() : "unknown";
            String name = rawEvent.getInvolvedObject() != null
                    ? rawEvent.getInvolvedObject().getName() : "unknown";

            String deployment = null;
            if ("Deployment".equals(kind)) {
                deployment = name;
            } else if ("ReplicaSet".equals(kind) && name != null && name.contains("-")) {
                deployment = name.substring(0, name.lastIndexOf("-"));
            }

            String component = rawEvent.getSource() != null && rawEvent.getSource().getComponent() != null
                        ? rawEvent.getSource().getComponent()
                        : (rawEvent.getReportingComponent() != null ? rawEvent.getReportingComponent() : "unknown");
            String host = rawEvent.getSource() != null && rawEvent.getSource().getHost() != null
                        ? rawEvent.getSource().getHost()
                        : "unknown";
            String action = rawEvent.getAction();

            K8sEvent entity = K8sEvent.builder()
                    .uid(uid)
                    .name(rawEvent.getMetadata().getName())
                    .namespace(namespace)
                    .reason(reason)
                    .type(type)
                    .message(message)
                    .involvedKind(kind)
                    .involvedName(name)
                    .sourceComponent(component)
                    .sourceHost(host)
                    .count(count)
                    .firstTimestamp(rawEvent.getFirstTimestamp() != null
                            ? rawEvent.getFirstTimestamp()
                            : rawEvent.getEventTime())
                    .lastTimestamp(rawEvent.getLastTimestamp())
                    .action(action)
                    .build();

            repository.save(entity);

            metricsService.incrementEventFull(namespace, type, kind, name, reason, component, host, deployment);

        } catch (DataIntegrityViolationException e) {
            log.debug("[WATCH] Duplicate event ignored (uid+count already exists)");
        } catch (Exception e) {
            log.error("[WATCH] Error saving event", e);
            metricsService.incrementError();
        }
    }

    // ---------------------------------------------------------
    // Helper
    // ---------------------------------------------------------
    private Integer safeCount(CoreV1Event event) {
        if (event.getCount() != null) {
            return event.getCount();
        }
        if (event.getSeries() != null && event.getSeries().getCount() != null) {
            return event.getSeries().getCount();
        }
        return 1;
    }

}
