package util;

public class TimeHandle {
	public static String formatTime(double milliseconds) {
		int seconds = (int) (milliseconds / 1000);
		int hours = seconds / 3600;
		seconds %= 3600;
		int minutes = seconds / 60;
		seconds %= 60;
		StringBuilder timeString = new StringBuilder();
		if (hours > 0) {
			timeString.append(hours).append(" hours ");
		}
		if (minutes > 0) {
			timeString.append(minutes).append(" minutes ");
		}
		timeString.append(String.format("%d seconds", seconds));
		return timeString.toString().trim();
	}

	public static double getCurrentTime() {
		return System.currentTimeMillis();
	}

}
