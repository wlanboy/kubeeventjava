package com.example.kubeevent.daten;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

public interface K8sEventRepository extends JpaRepository<K8sEvent, Long> {

    // Findet die letzten 100 Events
    @Transactional(readOnly = true)
    List<K8sEvent> findTop100ByOrderByCreatedAtDesc();

    // Suche mit Like über mehrere Felder
    @Transactional(readOnly = true)
    @Query("""
    SELECT e FROM K8sEvent e WHERE
        LOWER(e.message) LIKE LOWER(CONCAT('%', :q, '%')) OR
        LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
        LOWER(e.involvedName) LIKE LOWER(CONCAT('%', :q, '%')) OR
        LOWER(e.involvedKind) LIKE LOWER(CONCAT('%', :q, '%')) OR
        LOWER(e.namespace) LIKE LOWER(CONCAT('%', :q, '%')) OR
        LOWER(e.reason) LIKE LOWER(CONCAT('%', :q, '%')) OR
        LOWER(e.type) LIKE LOWER(CONCAT('%', :q, '%')) OR
        LOWER(e.sourceComponent) LIKE LOWER(CONCAT('%', :q, '%')) OR
        LOWER(e.sourceHost) LIKE LOWER(CONCAT('%', :q, '%'))
    """)
    Page<K8sEvent> searchEvents(@Param("q") String q, Pageable pageable);

    @Transactional
    int deleteByCreatedAtBefore(OffsetDateTime threshold);

    @Transactional(readOnly = true)
    boolean existsByUidAndCount(String uid, Integer count);
}