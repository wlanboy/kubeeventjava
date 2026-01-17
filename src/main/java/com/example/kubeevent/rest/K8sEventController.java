package com.example.kubeevent.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.example.kubeevent.daten.K8sEvent;
import com.example.kubeevent.daten.K8sEventRepository;

import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class K8sEventController {

    private static final int MAX_PAGE_SIZE = 100;

    private final K8sEventRepository repository;

    @GetMapping("/latest")
    public List<K8sEvent> latestEvents() {
        return repository.findTop100ByOrderByCreatedAtDesc();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<List<K8sEvent>> streamEvents(@RequestParam(defaultValue = "100") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(2))
                .map(sequence -> {
                    PageRequest pageRequest = PageRequest.of(0, safeLimit, Sort.by("createdAt").descending());
                    return repository.findAll(pageRequest).getContent();
                });
    }

    @GetMapping("/search")
    public Map<String, Object> searchEvents(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {

        // Validierung: page mindestens 1, page_size zwischen 1 und MAX_PAGE_SIZE
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(page_size, MAX_PAGE_SIZE));

        // app.js sendet page=1-basiert, Spring nutzt page=0-basiert
        PageRequest pageRequest = PageRequest.of(safePage - 1, safePageSize, Sort.by("createdAt").descending());
        Page<K8sEvent> resultPage;

        if (q != null && !q.isEmpty()) {
            resultPage = repository.searchEvents(q, pageRequest);
        } else {
            resultPage = repository.findAll(pageRequest);
        }

        return Map.of(
                "items", resultPage.getContent(),
                "total", resultPage.getTotalElements(),
                "page", safePage,
                "page_size", safePageSize,
                "pages", resultPage.getTotalPages());
    }
}