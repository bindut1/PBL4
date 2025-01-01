package downloadUI;

import java.util.List;
import util.*;
import view.MainUI;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class DownloadWaiting {
	private String url;
	private String filesize;
	private String savePath;
	private String time = "N/A";
	private String filename;
	private static List<DownloadWaiting> waitings = new ArrayList<>();
	private static boolean firstcall = true;

	public DownloadWaiting(String url, String filesize, String savePath, String time) {
		this.url = url;
		boolean checkTypeFile = url.endsWith(".torrent");
		this.savePath = savePath;
		this.time = time;

		try {
			long tmp = (checkTypeFile) ? FileHandle.getFileSizeTorrent(this.getUrl(), savePath)
					: FileHandle.getFileSizeFromConnectHttp(this.getUrl());
			this.filesize = (FileHandle.formatFileSize(tmp));
			
			String fileName = (checkTypeFile) ? FileHandle.getFileNameTorrent(url, savePath)
					: FileHandle.getFileNameFromConnectHttp(url);
			if (!checkTypeFile)
				fileName = FileHandle.ensureUniqueFileName(this.savePath, fileName);
			this.filename = fileName;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<DownloadWaiting> getListWaiting() {
		if (!firstcall)
			return waitings;
		firstcall = false;
		List<String> list = FileHandle.readFileFromTxt("WaitingFileTracking.txt");
		for (String i : list) {
			String[] parts = i.split(",");
			waitings.add(new DownloadWaiting(parts[0], parts[1], parts[2], parts[3]));
		}
		return waitings;
	}

	public static void addObjWaitingToList(String url, String filesize, String savePath, String time) {
		waitings.add(new DownloadWaiting(url, filesize, savePath, time));
	}

	public void updateTime(String time) {
		FileHandle.deleteLineFromTxtFile("WaitingFileTracking.txt", convertToStringTxt(this));
		this.time = time;
		FileHandle.saveFileWaitingToTxt(this.url, this.filesize, this.savePath, this.time);
		for (DownloadWaiting DownloadWaiting : waitings) {
			if (DownloadWaiting.url.equals(this.url)) {
				DownloadWaiting.setTime(time);
			}
		}
	}

	public static void addWaiting(String url, String sizefile, String path) {
		FileHandle.saveFileWaitingToTxt(url, sizefile, path, "N/A");
		waitings.add(new DownloadWaiting(url, sizefile, path, "N/A"));
	}
	
	public static void addWaitingWithDateCurrent(String url, String sizefile, String path) {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		String formattedTime = now.format(formatter);
		FileHandle.saveFileWaitingToTxt(url, sizefile, path, formattedTime);
		waitings.add(new DownloadWaiting(url, sizefile, path, formattedTime));
	}

	public static void deleteWaiting(String filename) {
		waitings.removeIf(objWaiting -> {
			if (objWaiting.getFileName().equals(filename)) {
				FileHandle.deleteLineFromTxtFile("WaitingFileTracking.txt", convertToStringTxt(objWaiting));
				return true;
			}
			return false;
		});

	}

	public static void handelWaiting(MainUI mainUI) {
		if (Downloading.getCountDownloading() >= Downloading.getMaxDownloading())
			return;
		waitings.removeIf(objWaiting -> {
			long waitingTimeMillis = TimeHandle.stringToTimeMillis(objWaiting.getTime());
			long currentTimeMillis = (long) TimeHandle.getCurrentTime();

			if (waitingTimeMillis != 0 && waitingTimeMillis <= currentTimeMillis)
				if (Downloading.getCountDownloading() < Downloading.getMaxDownloading()) {
					FileHandle.deleteLineFromTxtFile("WaitingFileTracking.txt", convertToStringTxt(objWaiting));
					Downloading.incrementCountDownloading();

					new Thread(() -> {
						Downloading downloading = new Downloading(objWaiting);
						MainUI.listFileDownloadingGlobal.add(downloading);
						mainUI.addDataToMainTable();
						downloading.start();
						Downloading.decrementCountDownloading();
					}).start();
					return true;
				}
			return false;
		});
	}

	private static String convertToStringTxt(DownloadWaiting waiting) {
		return String.format("%s,%s,%s,%s", waiting.getUrl(), waiting.getFilesize(), waiting.getSavePath(),
				waiting.getTime());
	}

	public String getFileName() {
		return this.filename;
	}

	public String getUrl() {
		return this.url;
	}

	public String getFilesize() {
		return this.filesize;
	}

	public String getSavePath() {
		return this.savePath;
	}

	public String getTime() {
		return this.time;
	}

	public void setTime(String time) {
		this.time = time;
	}

}
