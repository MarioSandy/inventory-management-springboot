package com.example.inventory_api_springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication()
@EnableJpaAuditing
@EnableScheduling
@ConfigurationPropertiesScan
public class InventoryApiSpringbootApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryApiSpringbootApplication.class, args);
	}
}
