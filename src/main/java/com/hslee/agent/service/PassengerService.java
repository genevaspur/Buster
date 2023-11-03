package com.hslee.agent.service;

import com.hslee.agent.repository.PassengerRepository;
import com.hslee.agent.repository.domain.Passenger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PassengerService {

	Logger logger = LoggerFactory.getLogger(PassengerService.class);

	@Autowired
	private PassengerRepository passengerRepository;

	/**
	 * 승객 생성
	 *
	 * @param passenger
	 * @return
	 */
	public Mono<Passenger> create(Passenger passenger) {
		return passengerRepository.save(passenger);
	}

	/**
	 * 승객 추가 / 수정
	 *
	 * @param passenger
	 * @return
	 */
	@Transactional
	public Mono<Passenger> save(final Passenger passenger) {
		return passengerRepository.findByUserId(passenger.getUserId())
				.flatMap(existingPassenger -> passengerRepository.save(existingPassenger.copyFrom(passenger)))
				.switchIfEmpty(Mono.defer(() -> {
					passenger.setNew(true);
					return passengerRepository.save(passenger);
				}));
	}

	/**
	 * 승객 삭제
	 * @param passenger
	 * @return
	 */
	@Transactional
	public Mono<Integer> delete(final Passenger passenger) {
		return passengerRepository.deleteByUserId(passenger.getUserId());
	}

	/**
	 * 승객 전체 조회
	 *
	 * @return
	 */
	public Flux<Passenger> findAll() {
		return passengerRepository.findAll();
	}
}
