package com.hslee.agent.repository;

import com.hslee.agent.repository.domain.Passenger;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PassengerRepository extends ReactiveCrudRepository<Passenger, Integer> {

	/**
	 * 사번으로 승객 조회
	 * @param userId
	 * @return
	 */
	Mono<Passenger> findByUserId(String userId);

	/**
	 * 사번으로 승객 삭제
	 * @param userId
	 * @return
	 */
	Mono<Integer> deleteByUserId(String userId);

}
