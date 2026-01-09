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

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

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

    private void setupApi() {
        this.factory = new SharedInformerFactory(apiClient);
        this.eventApi = new GenericKubernetesApi<>(
                CoreV1Event.class,
                CoreV1EventList.class,
                "", // Core API group
                "v1", // Version
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

            // Beim Start: vorhandene Events importieren
            importExistingEvents(targetNs);

            // Danach: Live-Watcher starten
            startInformer(targetNs);
        }

        log.info("[WATCH] Starting informer factory...");
        factory.startAllRegisteredInformers();
    }

    // ---------------------------------------------------------
    // EXISTING EVENTS IMPORT
    // ---------------------------------------------------------
    private void importExistingEvents(String namespace) {
        try {
            CoreV1EventList list = eventApi.list(namespace).getObject();
            if (list == null || list.getItems() == null) {
                return;
            }

            log.info("[WATCH] Importing {} existing events from namespace '{}'",
                    list.getItems().size(), namespace);

            for (CoreV1Event event : list.getItems()) {
                processIncomingEvent(event);
            }

        } catch (Exception e) {
            log.error("[WATCH] Failed to import existing events for '{}': {}", namespace, e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // LIVE WATCHER
    // ---------------------------------------------------------
    private void startInformer(String namespace) {
        SharedIndexInformer<CoreV1Event> informer = factory.sharedIndexInformerFor(
                eventApi,
                CoreV1Event.class,
                0L,
                namespace);

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
    private void processIncomingEvent(CoreV1Event rawEvent) {
        try {
            String uid = rawEvent.getMetadata().getUid();
            Integer count = safeCount(rawEvent);

            // Doppelte Events vermeiden
            if (repository.existsByUidAndCount(uid, count)) {
                return;
            }

            String namespace = rawEvent.getMetadata().getNamespace();
            String type = rawEvent.getType();
            String reason = rawEvent.getReason();
            String message = rawEvent.getMessage();

            String kind = rawEvent.getInvolvedObject().getKind();
            String name = rawEvent.getInvolvedObject().getName();

            // Deployment extrahieren (wie im Python-Exporter)
            String deployment = null;
            if ("ReplicaSet".equals(kind) && name.contains("-")) {
                deployment = name.substring(0, name.lastIndexOf("-"));
            }

            // Component extrahieren (wie im Python-Exporter)
            String component = extractComponent(rawEvent);

            // Event speichern
            K8sEvent entity = K8sEvent.builder()
                    .uid(uid)
                    .name(rawEvent.getMetadata().getName())
                    .namespace(namespace)
                    .reason(reason)
                    .type(type)
                    .message(message)
                    .involvedKind(kind)
                    .involvedName(name)
                    .count(count)
                    .firstTimestamp(rawEvent.getFirstTimestamp())
                    .lastTimestamp(rawEvent.getLastTimestamp())
                    .build();

            repository.save(entity);

            metricsService.incrementEventFull(
                    namespace,
                    type,
                    kind,
                    name,
                    reason,
                    component,
                    deployment);

        } catch (Exception e) {
            log.error("[WATCH] Error saving event: {}", e.getMessage());
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

    private String extractComponent(CoreV1Event event) {
        if (event.getSource() != null && event.getSource().getComponent() != null) {
            return event.getSource().getComponent();
        }
        return "unknown";
    }

}
