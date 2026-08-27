package com.example.kubeevent.daten;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.V1EventSource;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectReference;

import java.time.OffsetDateTime;

@Entity
@Table(name = "k8s_events",
    indexes = {
        @Index(name = "ix_k8sevent_uid_count", columnList = "uid, count"),
        @Index(name = "idx_namespace", columnList = "namespace"),
        @Index(name = "idx_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uc_uid_count", columnNames = {"uid", "count"})
    }
)
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class K8sEvent {

    private static final String UNKNOWN = "unknown";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uid;
    private String name;
    private String namespace;
    private String reason;
    private String type;
    
    @Column(columnDefinition = "TEXT")
    private String message;

    private String involvedKind;
    private String involvedName;

    private String sourceComponent;

    private String sourceHost;

    private String action;

    private OffsetDateTime firstTimestamp;
    private OffsetDateTime lastTimestamp;
    
    private Integer count;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static K8sEvent fromCoreV1Event(CoreV1Event rawEvent) {
        V1ObjectMeta metadata = rawEvent.getMetadata();
        V1ObjectReference involvedObject = rawEvent.getInvolvedObject();
        V1EventSource source = rawEvent.getSource();

        String kind = involvedObject != null ? involvedObject.getKind() : UNKNOWN;
        String name = involvedObject != null ? involvedObject.getName() : UNKNOWN;

        String component = source != null && source.getComponent() != null
                ? source.getComponent()
                : (rawEvent.getReportingComponent() != null ? rawEvent.getReportingComponent() : UNKNOWN);
        String host = source != null && source.getHost() != null ? source.getHost() : UNKNOWN;

        return K8sEvent.builder()
                .uid(metadata.getUid())
                .name(metadata.getName())
                .namespace(metadata.getNamespace())
                .reason(rawEvent.getReason())
                .type(rawEvent.getType())
                .message(rawEvent.getMessage())
                .involvedKind(kind)
                .involvedName(name)
                .sourceComponent(component)
                .sourceHost(host)
                .count(safeCount(rawEvent))
                .firstTimestamp(rawEvent.getFirstTimestamp() != null
                        ? rawEvent.getFirstTimestamp()
                        : rawEvent.getEventTime())
                .lastTimestamp(rawEvent.getLastTimestamp())
                .action(rawEvent.getAction())
                .build();
    }

    public static Integer safeCount(CoreV1Event rawEvent) {
        if (rawEvent.getCount() != null) {
            return rawEvent.getCount();
        }
        if (rawEvent.getSeries() != null && rawEvent.getSeries().getCount() != null) {
            return rawEvent.getSeries().getCount();
        }
        return 1;
    }
}