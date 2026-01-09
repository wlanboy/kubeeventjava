package com.example.kubeevent.jobs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "k8s")
@Getter
@Setter
public class WatcherConfig {
    
    // Spring splittet den String automatisch bei Kommas in eine Liste auf
    private List<String> watchedNamespaces;
}
