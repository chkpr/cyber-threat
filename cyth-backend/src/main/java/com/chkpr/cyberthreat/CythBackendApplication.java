package com.chkpr.cyberthreat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.chkpr.cyberthreat.config.RssProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(RssProperties.class)
public class CythBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CythBackendApplication.class, args);
	}

}
