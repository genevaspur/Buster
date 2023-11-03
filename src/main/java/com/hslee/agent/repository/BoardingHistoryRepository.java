package com.hslee.agent.repository;

import com.hslee.agent.repository.domain.BoardingHistory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface BoardingHistoryRepository extends ReactiveCrudRepository<BoardingHistory, Integer> {

	/**
	 * 탑승 이력 조회
	 * @param userId
	 * @param busNumber
	 * @param insertTime
	 * @return
	 */
	Mono<BoardingHistory> findByUserIdAndBusNumberAndInsertTime(String userId, String busNumber, LocalDateTime insertTime);

	/**
	 * 파일 저장 여부로 탑승 이력 조회
	 * @param saveYn
	 * @return
	 */
	Flux<BoardingHistory> findBySaveYn(String saveYn);

	/**
	 * 탑승 이력 생성
	 * @param userId
	 * @param busNumber
	 * @param insertTime
	 * @param passValidTime
	 * @param validYn
	 * @param backupYn
	 * @return
	 */
	@Query("INSERT INTO boarding_history (USER_ID, BUS_NUMBER, INSERT_TIME, PASS_VALID_TIME, VALID_YN, BACKUP_YN, SAVE_YN) " +
			"VALUES (:userId, :busNumber, :insertTime, :passValidTime, :validYn, :backupYn, :saveYn)")
	Mono<Integer> insert(String userId, String busNumber, LocalDateTime insertTime, LocalDateTime passValidTime, String validYn, String backupYn, String saveYn);

	/**
	 * 탑승 이력 수정
	 * @param userId
	 * @param busNumber
	 * @param insertTime
	 * @param passValidTime
	 * @param validYn
	 * @param backupYn
	 * @return
	 */
	@Query("UPDATE boarding_history " +
			"SET USER_ID = :userId, BUS_NUMBER = :busNumber, INSERT_TIME=:insertTime, PASS_VALID_TIME=:passValidTime, VALID_YN = :validYn, BACKUP_YN = :backupYn, SAVE_YN = :saveYn " +
			"WHERE USER_ID = :userId AND BUS_NUMBER = :busNumber AND INSERT_TIME = :insertTime")
	Mono<Integer> update(String userId, String busNumber, LocalDateTime insertTime, LocalDateTime passValidTime, String validYn, String backupYn, String saveYn);

}
