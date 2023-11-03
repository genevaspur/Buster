package com.hslee.agent.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Table("boarding_history")
public class BoardingHistory {

	@Column("USER_ID")
	private String userId;

	@Column("BUS_NUMBER")
	private String busNumber;

	@Column("INSERT_TIME")
	private LocalDateTime insertTime;

	@Column("PASS_VALID_TIME")
	private LocalDateTime passValidTime;

	@Column("VALID_YN")
	private String validYn;

	@Column("BACKUP_YN")
	private String backupYn;

	@Column("SAVE_YN")
	private String saveYn;

	public BoardingHistory copyFrom(BoardingHistory source) {
		return new BoardingHistory(source.getUserId(), source.getBusNumber(), source.getInsertTime(), source.getPassValidTime(), source.getValidYn(), source.getBackupYn(), source.getSaveYn());
	}
}
