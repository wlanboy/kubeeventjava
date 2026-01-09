package com.example.kubeevent.jobs;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;

@Configuration
public class K8sConfig {
    @Bean
    public ApiClient apiClient() throws IOException {
        // Erkennt automatisch, ob im Cluster (ServiceAccount) 
        // oder lokal (kubeconfig) gestartet wurde
        ApiClient client = Config.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        return client;
    }
}