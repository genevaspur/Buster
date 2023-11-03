package com.hslee.agent.service;

import com.hslee.agent.repository.BoardingHistoryRepository;
import com.hslee.agent.repository.PassengerRepository;
import com.hslee.agent.repository.domain.BoardingHistory;
import com.hslee.agent.repository.domain.Passenger;
import com.hslee.agent.model.CommuteModel;
import com.hslee.agent.utils.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class BoardingHistoryService {
	@Autowired
	private BoardingHistoryRepository boardingHistoryRepository;

	@Autowired
	private PassengerRepository passengerRepository;

//	private static final Clip OK_CLIP;
//	static {
//		try {
//			InputStream OK_SOUND = new ClassPathResource("static/sound/sound_ok.wav").getInputStream();
//			InputStream buffer = new BufferedInputStream(OK_SOUND);
//			OK_CLIP = AudioSystem.getClip();
//			OK_CLIP.open(AudioSystem.getAudioInputStream(buffer));
//			buffer.close();
//			OK_SOUND.close();
//
//
//
////			URL url = new ClassPathResource("static/sound/sound_ok.wav").getURL();
////			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
////			AudioFormat format = audioInputStream.getFormat();
////			DataLine.Info lineInfo = new DataLine.Info(Clip.class, format);
////			Mixer.Info selectedMixer = null;
////			for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
////				Mixer mixer = AudioSystem.getMixer(mixerInfo);
////				if (mixer.isLineSupported(lineInfo)) {
////					selectedMixer = mixerInfo;
////					break;
////				}
////			}
////			if (selectedMixer != null) {
////				OK_CLIP = AudioSystem.getClip(selectedMixer);
////				OK_CLIP.open(audioInputStream);
////			} else {
////				throw new RuntimeException("No suitable mixers found for line info: " + lineInfo);
////			}
//
//
//
//
//		} catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	private static final Clip NO_CLIP;
//	static {
//		try {
//			InputStream NO_SOUND = new ClassPathResource("static/sound/sound_no.wav").getInputStream();
//			InputStream buffer = new BufferedInputStream(NO_SOUND);
//			NO_CLIP = AudioSystem.getClip();
//			NO_CLIP.open(AudioSystem.getAudioInputStream(buffer));
//			buffer.close();
//			NO_SOUND.close();
//		} catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
//			throw new RuntimeException(e);
//		}
//	}

	/**
	 * 탑승 이력 생성
	 *
	 * @param boardingHistory
	 * @return
	 */
	public Mono<BoardingHistory> create(BoardingHistory boardingHistory) {
		return boardingHistoryRepository.save(boardingHistory);
	}

	/**
	 * 탑승 이력 수정
	 *
	 * @param boardingHistory
	 * @return
	 */
	@Transactional
	public Mono<Integer> updateBoardingHistory(final BoardingHistory boardingHistory) {
		return boardingHistoryRepository.update(boardingHistory.getUserId(), boardingHistory.getBusNumber(), boardingHistory.getInsertTime(), boardingHistory.getPassValidTime(), boardingHistory.getValidYn(), boardingHistory.getBackupYn(), boardingHistory.getSaveYn());
//		return boardingHistoryRepository.findByUserIdAndBusNumberAndInsertTime(boardingHistory.getUserId(), boardingHistory.getBusNumber(), boardingHistory.getInsertTime())
//				.flatMap(existingHistory -> {
//					BoardingHistory temp = existingHistory.copyFrom(boardingHistory);
//					return boardingHistoryRepository.update(temp.getUserId(), temp.getBusNumber(), temp.getInsertTime(), temp.getPassValidTime(), temp.getValidYn(), temp.getBackupYn(), temp.getSaveYn());
//				});
	}

	/**
	 * 탑승 이력 빠른 수정
	 *
	 * @param boardingHistory
	 * @return
	 */
	@Transactional
	public Mono<Integer> simpleUpdate(final BoardingHistory boardingHistory) {
		return boardingHistoryRepository.update(boardingHistory.getUserId(), boardingHistory.getBusNumber(), boardingHistory.getInsertTime(), boardingHistory.getPassValidTime(), boardingHistory.getValidYn(), boardingHistory.getBackupYn(), boardingHistory.getSaveYn());
	}

	/**
	 * 탑승 이력 전체 조회
	 *
	 * @return
	 */
	public Flux<BoardingHistory> findAll() {
		return boardingHistoryRepository.findAll();
	}

	/**
	 * 서버 저장 여부로 탑승 이력 조회
	 *
	 * @param saveYn
	 * @return
	 */
	public Flux<BoardingHistory> findBySaveYn(String saveYn) {
		return boardingHistoryRepository.findBySaveYn(saveYn);
	}

	/**
	 * 출퇴근 태그 유효성 검증
	 *
	 * @param commuteModel
	 * @return
	 */
	public CommuteModel commuteValidation(CommuteModel commuteModel) {
		// 1. 승객 유효성 검증
		boolean isNotValid = false;
		Passenger passenger = passengerRepository.findByUserId(commuteModel.getUserId()).block();
		if (passenger == null) {
			isNotValid = true;
			commuteModel.setValid(false);
			commuteModel.setMessage("미등록된 탑승자입니다.");
		}

		// 2. 유효 시간 검증
		LocalDateTime validDate = DateUtil.toLocalDateTime(commuteModel.getPassValidTime());
		LocalDateTime now = LocalDateTime.now();

		// 2-1. 라즈베리 시간과 QR 서버의 현재 시간의 차이가 3분 이상 이면 QR 코드의 현재 시간 사용
		LocalDateTime currentRemoteTime =  DateUtil.toLocalDateTime(commuteModel.getCurrentRemoteTime());
		if (currentRemoteTime.isBefore(now.minusMinutes(3)) || currentRemoteTime.isAfter(now.plusMinutes(3))) {
			now = currentRemoteTime;
		}

		if (!isNotValid) {
			if (validDate.isBefore(now)) {
				commuteModel.setValid(false);
				commuteModel.setMessage("QR 코드의 유효시간이 지났습니다.");
			} else {
				commuteModel.setValid(true);
				commuteModel.setMessage(passenger.getTeamNm() + " " + passenger.getUserNm() + "님 환영합니다.");
			}
		}

		// 2-1. 사운드 재생
//		playAudio(commuteModel.isValid());

		// 3. 탑승 이력 비동기로 저장
		insertBoardingHistory(commuteModel, now, validDate);
		return commuteModel;
	}

	private void insertBoardingHistory(CommuteModel commuteModel, LocalDateTime nowDate, LocalDateTime validDate) {
		String validYn = commuteModel.isValid() ? "Y" : "N";
		// TODO 차량번호 어떻게??
		String carNo = "40너1234";
		boardingHistoryRepository.insert(commuteModel.getUserId(), carNo, nowDate, validDate, validYn, "N", "N")
				.subscribe();
	}

//	private static void playAudio(boolean isValid) {
//		if (isValid) {
//			OK_CLIP.start();
//		} else {
//			NO_CLIP.start();
//		}
//	}
}
