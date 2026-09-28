package com.raj.embeddingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;


@EnableRetry
@ConfigurationPropertiesScan
@SpringBootApplication
public class EmbeddingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmbeddingServiceApplication.class, args);
	}

}
