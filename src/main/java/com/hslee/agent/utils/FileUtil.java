package com.hslee.agent.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class FileUtil {
	public static String detectEncoding(String filePath) throws IOException {
		String[] encodings = {"UTF-8", "EUC-KR", "ISO-8859-1", "CP949", "MS949", "UTF-16", "UTF-32"};
		for (String encoding : encodings) {
			try (InputStreamReader reader = new InputStreamReader(new FileInputStream(filePath), encoding)) {
				// 파일을 읽어올 때 에러 없이 읽어왔다면 해당 인코딩으로 판단
				return encoding;
			} catch (UnsupportedEncodingException e) {
				// 해당 인코딩으로 읽을 수 없는 경우, 다음 인코딩 시도
			}
		}
		return "Unknown"; // 모든 시도가 실패한 경우
	}
}
