package com.hslee.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BusterAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(BusterAgentApplication.class, args);
	}

}
