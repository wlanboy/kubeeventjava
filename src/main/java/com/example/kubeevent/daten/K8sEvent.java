package com.example.kubeevent.daten;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;

@Entity
@Table(name = "k8s_events", indexes = {
    @Index(name = "ix_k8sevent_uid_count", columnList = "uid, count"),
    @Index(name = "idx_namespace", columnList = "namespace"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class K8sEvent {

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

    private OffsetDateTime firstTimestamp;
    private OffsetDateTime lastTimestamp;
    
    private Integer count;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}