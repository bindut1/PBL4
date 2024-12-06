package view;

import java.util.List;

import net.bytebuddy.asm.Advice.This;
import util.*;
import java.util.ArrayList;

public class objWaiting {
	private String url;
	private String filesize;
	private String savePath;
	private String time = "N/A";
	private String filename;
	private static List<objWaiting> waitings = new ArrayList<>();
	private static boolean firstcall = true;

	public objWaiting(String url, String filesize, String savePath, String time) {
		this.url = url;
		this.filesize = filesize;
		this.savePath = savePath;
		this.time = time;
		boolean checkTypeFile = url.endsWith(".torrent");
		try {
			String fileName = (checkTypeFile) ? FileHandle.getFileNameTorrent(url, savePath)
					: FileHandle.getFileNameFromConnectHttp(url);
			if (!checkTypeFile) fileName = FileHandle.ensureUniqueFileName(this.savePath, fileName);
			this.filename = fileName;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<objWaiting> getListWaiting() {
		if (!firstcall)
			return waitings;
		firstcall = false;
		List<String> list = FileHandle.readFileFromTxt("WaitingFileTracking.txt");
		for (String i : list) {
			String[] parts = i.split(",");
			waitings.add(new objWaiting(parts[0], parts[1], parts[2], parts[3]));
		}
		return waitings;
	}

	public static void addObjWaitingToList(String url, String filesize, String savePath, String time) {
		waitings.add(new objWaiting(url, filesize, savePath, time));
	}

	public void updateTime(String time) {
		FileHandle.deleteLineFromTxtFile("WaitingFileTracking.txt", convertToStringTxt(this));
		this.time = time;
		FileHandle.saveFileWaitingToTxt(this.url, this.filesize, this.savePath, this.time);
		for (objWaiting objWaiting : waitings) {
			if (objWaiting.url.equals(this.url)) {
				objWaiting.setTime(time);
			}
		}
	}

	public static void addWaiting(String url, String sizefile, String path) {
		FileHandle.saveFileWaitingToTxt(url, sizefile, path, "N/A");
		waitings.add(new objWaiting(url, sizefile, path, "N/A"));
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
		List<UIObjectGeneral> downloadFiles = new ArrayList<UIObjectGeneral>();
		waitings.removeIf(objWaiting -> {
			long waitingTimeMillis = TimeHandle.stringToTimeMillis(objWaiting.getTime());
			long currentTimeMillis = (long) TimeHandle.getCurrentTime();
			if (waitingTimeMillis != 0 && waitingTimeMillis <= currentTimeMillis) {
				downloadFiles.add(new UIObjectGeneral(objWaiting));
				FileHandle.deleteLineFromTxtFile("WaitingFileTracking.txt", convertToStringTxt(objWaiting));
				return true;
			}
			return false;
		});
		if (!downloadFiles.isEmpty()) {
			for (UIObjectGeneral uiObjectGeneral : downloadFiles) {
				new Thread(() -> {
					MainUI.listFileDownloadingGlobal.add(uiObjectGeneral);
					mainUI.addDataToMainTable();
					uiObjectGeneral.start();
				}).start();
			}
		}
	}

	private static String convertToStringTxt(objWaiting waiting) {
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
