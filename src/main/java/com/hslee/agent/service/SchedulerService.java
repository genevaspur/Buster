package com.hslee.agent.service;

import com.hslee.agent.repository.domain.BoardingHistory;
import com.hslee.agent.model.BoardingHistoryModel;
import com.hslee.agent.model.JsonResponse;
import com.hslee.agent.model.PassengerModel;
import com.hslee.agent.utils.DateUtil;
import io.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchedulerService {
	Logger logger = LoggerFactory.getLogger(SchedulerService.class);

	@Value("${buster.target.url}")
	private String targetUrl;

	@Value("${buster.target.file-path}")
	private String filePath;

	@Value("${buster.target.back-up-path}")
	private String backUpPath;

	@Value("${buster.target.config-path}")
	private String configPath;

	@Autowired
	private BoardingHistoryService boardingHistoryService;

	@Autowired
	private PassengerService passengerService;

	@Autowired
	WebClient webClient;

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");

	/**
	 * 1분 주기로 파일 생성
	 */
	@Scheduled(fixedRateString = "${buster.agent.schedule.backup}")
	public void backUpCommuteData() {
		logger.info("Creating backup file");

		try {
			// 1. DB -> boarding_history -> saveYn = 'N' 조회
			List<BoardingHistory> boardingHistoryList = boardingHistoryService.findBySaveYn("N")
					.collectList()
					.block();

			// 2. yyyyMMddHHmm 형식의 txt 파일 생성
			if (boardingHistoryList != null && !boardingHistoryList.isEmpty()) {
				try (Writer out = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(filePath + "/" + sdf.format(new Date()) + ".txt"), StandardCharsets.UTF_8))) {
					StringBuilder content = new StringBuilder();

					// ex) 1742490137,40너1234,2023-09-04 10:59:20,2023-09-04 10:59:20,Y
					for (BoardingHistory boardingHistory : boardingHistoryList) {
						String insertTime = boardingHistory.getInsertTime().format(DateUtil.DATE_TIME_FORMATTER);
						String passValidTime = boardingHistory.getPassValidTime().format(DateUtil.DATE_TIME_FORMATTER);
						content.append(boardingHistory.getUserId()).append(",").append(boardingHistory.getBusNumber()).append(",").append(insertTime).append(",").append(passValidTime).append(",").append(boardingHistory.getValidYn()).append("\n");

						// DB -> saveYn = 'Y'로 변경
						boardingHistory.setSaveYn("Y");
						boardingHistoryService.simpleUpdate(boardingHistory)
								.subscribe();
					}

					out.write(content.toString());
				} catch (Exception e) {
					logger.error("ERROR :::: Cannot create file {}", e.getMessage());
				}
			}

		} catch (Exception e) {
			logger.error("ERROR :::: backUpCommuteData {}", e.getMessage());
		}
	}

	public void passengerDateTransfer() {
		try {
			// Buster 서버 통신 가능 여부 확인
			HttpStatusCode statusCode = pingHost();
			if (statusCode == HttpStatus.OK) {
				logger.info("Successfully ping-pong to Buster server");

				 // 1. agent.txt 파일 읽어서 마지막 구동 날짜 가져오기
				String lastServiceStartDate = readAgentFile();

				// 2. Buster 서버로 구동 날짜 보내기
				getPassengerData(lastServiceStartDate);
			} else {
				// 통신 불가능하면 pass
				logger.error("ERROR :::: Cannot connect to buster server. status code : {}", statusCode);
			}
		} catch (WebClientRequestException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof ReadTimeoutException) {
				logger.error("buster server is not responding.......");
			} else {
				logger.error("ERROR :::: Network {}", ex.getMessage());
				// 30초 후 재시도
				try {
					Thread.sleep(30000);
					passengerDateTransfer();
				} catch (InterruptedException e) {
				}
			}
		} catch (Exception e) {
			logger.error("ERROR :::: passengerDateTransfer {}", e.getMessage());
		}
	}

	/**
	 * 출퇴근 정보 전송 스케줄러
	 * 1. buster 서버 통신 가능 여부 확인
	 * 2. 출퇴근 파일 읽음
	 * 3. buster 서버에 전송
	 */
	// TODO Backup 파일 정리 스케줄러
//	@Scheduled(fixedDelayString = "${buster.agent.schedule.commute}")
	public void commuteDataTransfer() {
		logger.info("Sending commute information to {}", targetUrl);

		try {
			// 1. buster 서버 통신 가능 여부 확인
			HttpStatusCode statusCode = pingHost();
			if (statusCode == HttpStatus.OK) {
				logger.info("Successfully ping-pong to buster server");

				// 2-1. 통신 가능하면 출퇴근 파일 읽음
				Map<File, String> fileContentMap = readFilesInFolder();

				// 3. 전송할 파일이 없으면 pass
				if (fileContentMap == null || fileContentMap.isEmpty()) {
					logger.info("No files to send");
					return;
				}

				// 4. buster 서버에 전송
				initFileTransfer(fileContentMap);
			} else {
				// 2-2. 통신 불가능하면 pass
				logger.error("ERROR :::: Cannot connect to buster server. status code : {}", statusCode);
			}
		} catch (WebClientRequestException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof ReadTimeoutException) {
				logger.error("buster server is not responding.......");
			} else {
				logger.error("ERROR :::: Network {}", ex.getMessage());
			}
		} catch (Exception e) {
			logger.error("ERROR :::: commuteDataTransfer {}", e.getMessage());
		}
	}

	/**
	 * buster 서버에서 승객 정보 가져오기
	 * @param lastServiceStartDate
	 */
	private void getPassengerData(String lastServiceStartDate) {
		webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/commute/getPassenger.do")
						.queryParamIfPresent("lastDay", Optional.ofNullable(lastServiceStartDate))
						.build()
				)
				.retrieve()
				.bodyToMono(JsonResponse.class)
				.subscribe(response -> {
					// 3. 가져온 승객 정보 가공(update, delete)
					handlePassengerData(response);

					// 4. 현재 날짜를 agent.txt 파일에 작성 하기
					saveNewDateToFile();
				});
	}

	/**
	 * 현재날짜를 agent.txt 파일에 작성하기
	 * yyyy-MM-dd
	 */
	private void saveNewDateToFile() {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(configPath + "agent.txt"));
			bw.write(LocalDate.now().toString());
			bw.close();
		} catch (IOException e) {
			logger.error("Failed to write agent.txt file: " + e.getMessage());
		}
	}

	/**
	 * 가져온 승객 정보 가공(update, delete)
	 * @param response
	 */
	private void handlePassengerData(JsonResponse response) {
		List<PassengerModel> updateList = response.getUpdate();
		List<PassengerModel> deleteList = response.getDelete();

		// 3-1. 승객 정보 save
		for (PassengerModel passengerModel : updateList) {
			passengerService.save(passengerModel.toEntity()).subscribe(result -> {
				if (result == null) {
					logger.error("Cannot save passenger : {}", passengerModel);
				}
			});
		}

		// 3-2. 승객 정보 delete
		for (PassengerModel passengerModel : deleteList) {
			passengerService.delete(passengerModel.toEntity()).subscribe(result -> {
				if (result != 1) {
					logger.error("Cannot delete passenger : {}", passengerModel);
				} else {
					logger.info("Successfully deleted passenger : {}", passengerModel);
				}
			});
		}
	}

	/**
	 * agent.txt 파일 읽어서 마지막 구동 날짜 가져오기
	 * @return null or String
	 * @throws IOException
	 */
	private String readAgentFile() throws IOException {
		File file = new File(configPath + "agent.txt");
		file.setReadable(true, false);
		file.setExecutable(true, false);
		file.setWritable(true, false);
		if (!file.exists()) {
			try {
				boolean created = file.createNewFile();
				if (!created) {
					throw new IOException("Failed to create agent.txt file");
				}
			} catch (IOException e) {
				logger.error("Failed to create agent.txt file: " + e.getMessage());
			}
			return null;
		} else {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String lastDate = br.readLine();
			br.close();
			return lastDate;
		}
	}

	/**
	 * buster 서버에 출퇴근 정보 전송
	 *
	 * @param fileContentMap
	 */
	private void initFileTransfer(Map<File, String> fileContentMap) {
		for (Map.Entry<File, String> entry : fileContentMap.entrySet()) {
			File file = entry.getKey();
			String content = entry.getValue();
			List<BoardingHistoryModel> boardingHistoryModelList = new ArrayList<>();

			try {
				for (String row : content.split("\n")) {
					String[] split = row.split(",");
					String userId = split[0];
					String busNumber = split[1];
					String insertTime = split[2];
					String passValidTime = split[3];
					String validYn = split[4];
					boardingHistoryModelList.add(new BoardingHistoryModel(userId, busNumber, insertTime, passValidTime, validYn));
				}
				sendCommuteInfo(boardingHistoryModelList, file);
			} catch (ArrayIndexOutOfBoundsException e) {
				// 파일 내용이 잘못된 경우 pass
				logger.error("{} file is not valid. {}", file.getName(), e.getMessage());
			}
		}
	}

	/**
	 * buster 서버 통신 가능 여부 확인
	 *
	 * @return 상태코드
	 */
	private HttpStatusCode pingHost() {
		return webClient.get()
				.uri("/commute/ping.do")
				.exchangeToMono(response -> {
					HttpStatusCode httpStatusCode = response.statusCode();
					return Mono.just(httpStatusCode);
				}).block();
	}

	/**
	 * buster 서버에 출퇴근 정보 전송 후 파일 백업
	 *
	 * @param boardingHistoryModelList
	 */
	private void sendCommuteInfo(List<BoardingHistoryModel> boardingHistoryModelList, File file) {
		webClient.post()
				.uri("/commute/insertCommute.do")
				.bodyValue(boardingHistoryModelList)
				.exchangeToMono(response -> {
					HttpStatusCode httpStatusCode = response.statusCode();
					return Mono.just(httpStatusCode);
				})
				.subscribe(httpStatusCode -> {
					if (httpStatusCode == HttpStatus.OK) {
						// 전송 완료되면 파일 백업
						logger.info("Successfully sent commute information to buster server");
						backUpFile(boardingHistoryModelList, file);
					} else {
						logger.error("ERROR :::: Failed to send commute information to buster server. status code : {}", httpStatusCode);
					}
				});
	}

	/**
	 * 전송 완료한 파일 백업 및 DB 업데이트
	 *
	 * @param boardingHistoryModelList 파일 내용
	 * @param file                     파일
	 */
	private void backUpFile(List<BoardingHistoryModel> boardingHistoryModelList, File file) {
		try {
			// 1. 파일 이동
			Files.move(Path.of(file.getPath()), Path.of(backUpPath + file.getName()));
			// 2. MariaDB -> boarding_history -> backUpYn = 'Y'
			for (BoardingHistoryModel boardingHistoryModel : boardingHistoryModelList) {
				BoardingHistory boardingHistory = boardingHistoryModel.toEntity("Y");
				boardingHistoryService.updateBoardingHistory(boardingHistory).subscribe(result -> {
					if (result == 1) {
						logger.info("Successfully updated boarding history : {}", boardingHistory);
					} else {
						logger.error("ERROR :::: Failed to update boarding history : {}", boardingHistory);
					}
				});
			}
			logger.info("Successfully back-up file : {}", file.getPath());
		} catch (Exception e) {
			logger.error("ERROR :::: Failed to back-up file : {} {}", file.getPath(), e.getMessage());
		}
	}

	/**
	 * 출퇴근 파일 읽기
	 *
	 * @return Map<File, String> 파일 및 해당 파일의 내용 Map
	 * @throws IOException filePath 경로 존재 안함
	 */
	private Map<File, String> readFilesInFolder() throws IOException {
		File folder = new File(filePath);

		if (folder.exists() && folder.isDirectory()) {
			File[] files = folder.listFiles();
			if (files == null || files.length == 0) {
				return null;
			}

			// 파일 이름으로 정렬
			Arrays.sort(files);

			// 파일 읽기 (UTF-8 인코딩 사용)
			return Arrays.stream(files)
					.filter(file -> file.isFile() && file.getName().endsWith(".txt"))
					.collect(Collectors.toMap(file -> file, file -> {
						try {
							logger.info("Reading file : {}", file.getName());
							return Files.readString(file.toPath(), StandardCharsets.UTF_8);
						} catch (IOException e) {
							logger.error("ERROR :::: readFilesInFolder {}", e.getMessage());
							return "ERROR";
						}
					}));
		} else {
			throw new IOException("Folder not found");
		}
	}

}
