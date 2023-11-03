package com.hslee.agent.model;

import com.hslee.agent.repository.domain.BoardingHistory;
import com.hslee.agent.utils.DateUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeParseException;

@Getter
@Setter
@AllArgsConstructor
public class BoardingHistoryModel {
	private String userId;
	private String busNumber;
	private String insertTime;
	private String passValidTime;
	private String validYn;

	public BoardingHistory toEntity(String backupYn) throws DateTimeParseException {
		return new BoardingHistory(userId, busNumber, DateUtil.toLocalDateTime(insertTime), DateUtil.toLocalDateTime(passValidTime), validYn, backupYn, backupYn);
	}
}
