package util;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
public class TimeHandle {
	public static String formatTime(double milliseconds) {
		int seconds = (int) (milliseconds / 1000);
		int hours = seconds / 3600;
		seconds %= 3600;
		int minutes = seconds / 60;
		seconds %= 60;
		StringBuilder timeString = new StringBuilder();
		if (hours > 0) {
			timeString.append(hours).append("h");
		}
		if (minutes > 0) {
			timeString.append(minutes).append("m");
		}
		timeString.append(String.format("%ds", seconds));
		return timeString.toString().trim();
	}

	public static double getCurrentTime() {
		return System.currentTimeMillis();
	}
	
    public static long stringToTimeMillis(String dateTimeStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);
            long milliseconds = dateTime.atZone(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli();

            return milliseconds;
        } catch (Exception e) {
            return 0;
        }
    }

}
