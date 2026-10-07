package com.ga.gymio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GymioApplication {

	public static void main(String[] args) {
		SpringApplication.run(GymioApplication.class, args);
	}

}
