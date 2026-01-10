package com.example.kubeevent;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.kubernetes.client.spring.extended.controller.config.KubernetesReconcilerAutoConfiguration;

@SpringBootApplication(exclude = { 
    KubernetesReconcilerAutoConfiguration.class 
})
@EnableScheduling
public class KubeeventApplication {

    public static void main(String[] args) {

        // Ordner ./data anlegen, falls nicht vorhanden
        File dataDir = new File("./data");
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (!created) {
                System.err.println("Konnte ./data nicht anlegen!");
            }
        }

        SpringApplication.run(KubeeventApplication.class, args);
    }
}

