package com.chkpr.cyberthreat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CythBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CythBackendApplication.class, args);
	}

}
