package com.veo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EyewearBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(EyewearBackendApplication.class, args);
	}

}
