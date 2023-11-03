package com.hslee.agent.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public static LocalDateTime toLocalDateTime(String time) {
		return LocalDateTime.parse(time, DATE_TIME_FORMATTER);
	}
}
