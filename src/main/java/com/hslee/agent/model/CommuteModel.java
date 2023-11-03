package com.hslee.agent.model;

import lombok.Data;

@Data
public class CommuteModel {
	private String compCd;

	private String userId;

	private String gps;

	private String passValidTime;

	private boolean isValid;

	private String message;

	private String currentRemoteTime;
}
