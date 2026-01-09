package com.example.kubeevent;

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
		SpringApplication.run(KubeeventApplication.class, args);
	}

}
