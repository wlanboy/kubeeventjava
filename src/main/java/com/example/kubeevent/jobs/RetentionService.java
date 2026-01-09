package com.example.kubeevent.jobs;

import com.example.kubeevent.daten.K8sEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionService {

    private final K8sEventRepository repository;

    // "0 * * * * *" entspricht jeder Minute oder "0 0 * * * *" für jede Stunde
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void runRetention() {
        log.info("[CRON] Running retention job...");
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(7);
        int deleted = repository.deleteByCreatedAtBefore(threshold);
        log.info("[CRON] Retention job complete. Deleted {} events.", deleted);
    }
}