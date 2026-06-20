package com.example.KendyDigital;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class KendyDigitalApplication {
	@org.springframework.context.annotation.Bean
	Clock systemClock() {
		return Clock.systemUTC();
	}

	public static void main(String[] args) {
		SpringApplication.run(KendyDigitalApplication.class, args);
	}

}
