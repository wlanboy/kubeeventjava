package com.example.kubeevent.daten;

public record EventOccurrence(
        String namespace,
        String type,
        String kind,
        String name,
        String reason,
        String component,
        String host,
        String deployment) {
}
