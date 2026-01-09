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

    private final K8sEventRepository repository;

    @GetMapping("/events/latest")
    public List<K8sEvent> latestEvents() {
        return repository.findTop100ByOrderByCreatedAtDesc();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<List<K8sEvent>> streamEvents(@RequestParam(defaultValue = "100") int limit) {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(2))
                .map(sequence -> {
                    // Wir holen die neuesten Events (limit beachten)
                    PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("createdAt").descending());
                    return repository.findAll(pageRequest).getContent();
                })
                .log();
    }

    @GetMapping("/search")
    public Map<String, Object> searchEvents(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {

        // app.js sendet page=1-basiert, Spring nutzt page=0-basiert
        PageRequest pageRequest = PageRequest.of(page - 1, page_size, Sort.by("createdAt").descending());
        Page<K8sEvent> resultPage;

        if (q != null && !q.isEmpty()) {
            resultPage = repository.searchEvents(q, pageRequest);
        } else {
            resultPage = repository.findAll(pageRequest);
        }

        return Map.of(
                "items", resultPage.getContent(),
                "total", resultPage.getTotalElements(),
                "page", page,
                "page_size", page_size,
                "pages", resultPage.getTotalPages());
    }
}