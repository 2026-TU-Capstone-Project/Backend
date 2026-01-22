package com.example.Capstone_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableJpaAuditing
public class CapstoneProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(CapstoneProjectApplication.class, args);
	}

}
