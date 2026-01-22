package com.example.kubeevent.jobs;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "k8s")
@Validated
@Getter
@Setter
@Slf4j
public class WatcherConfig {

    // Spring splittet den String automatisch bei Kommas in eine Liste auf
    @NotEmpty(message = "k8s.watched-namespaces must not be empty. Set via environment variable K8S_WATCHED_NAMESPACES or application.properties")
    private List<String> watchedNamespaces;

    @PostConstruct
    public void validate() {
        if (watchedNamespaces != null) {
            // Entferne leere Strings und trimme
            watchedNamespaces = watchedNamespaces.stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();

            if (watchedNamespaces.isEmpty()) {
                throw new IllegalStateException(
                        "k8s.watched-namespaces contains only empty values. Please configure valid namespace names.");
            }

            log.info("[CONFIG] Configured namespaces to watch: {}", watchedNamespaces);
        }
    }
}
