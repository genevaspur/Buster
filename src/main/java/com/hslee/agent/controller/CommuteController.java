package com.hslee.agent.controller;

import com.hslee.agent.model.CommuteModel;
import com.hslee.agent.service.BoardingHistoryService;
import com.hslee.agent.service.SchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommuteController {

	@Autowired
	private BoardingHistoryService boardingHistoryService;

	@Autowired
	private SchedulerService schedulerService;

	@PostMapping("/commute")
	public ResponseEntity<CommuteModel> commute(@RequestBody CommuteModel commuteModel) {
		try {
			CommuteModel result = boardingHistoryService.commuteValidation(commuteModel);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (Exception e) {
			CommuteModel error = new CommuteModel();
			error.setMessage(e.getMessage());
			error.setValid(false);
			return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/passenger-data-transfer")
	public ResponseEntity<String> passengerDataTransfer() {
		try {
			schedulerService.passengerDateTransfer();
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/commute-data-transfer")
	public ResponseEntity<String> commuteDataTransfer() {
		try {
			schedulerService.commuteDataTransfer();
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
