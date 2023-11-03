package com.hslee.agent.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupService implements ApplicationRunner {

	@Autowired
	SchedulerService schedulerService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		schedulerService.passengerDateTransfer();
	}
}
