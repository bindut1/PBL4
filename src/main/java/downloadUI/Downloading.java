package downloadUI;

import download.DownloadObject;
import javafx.application.Platform;
import view.ProgressUI;
import view.MainUI;
import utilUI.*;
import util.FileHandle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Downloading extends DownloadObject {
	private static int maxDownloading = 5;
	private static int countDownloading = 0;

	private String url;
	private String path;
	private String fileName;
	private String status;
	private String fileSize;
	private String date;
	private String time;
	private boolean selected = false;
	private boolean isSaveToTxt = false;

	public Downloading(String url, String path) {
		super(url, path);
	}

	public Downloading(DownloadWaiting waiting) {
		super(waiting.getUrl(), waiting.getSavePath());
		this.fileName = waiting.getFileName();
		this.date = waiting.getTime();
		this.fileSize = waiting.getFilesize();
		this.path = waiting.getSavePath();
		this.downloader.setFileName(fileName);
	}

	public void updateProgressUI(ProgressUI progressUI) {
		if (this.downloaderNotNull() && this.downloader.getRunningFlag()) {
			Platform.runLater(() -> {
				progressUI.updateProgress(this.downloader.getProgress());
				progressUI.appendText(this.downloader.getDetailText());
			});
		}
	}

	public void updateInfor() {
		try {
			String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			boolean checkTypeFile = this.getUrl().endsWith(".torrent");
			String fileName = (checkTypeFile) ? FileHandle.getFileNameTorrent(this.getUrl(), this.getPath())
					: FileHandle.getFileNameFromConnectHttp(this.getUrl());
			long fileSize = (checkTypeFile) ? FileHandle.getFileSizeTorrent(this.getUrl(), this.getPath())
					: FileHandle.getFileSizeFromConnectHttp(this.getUrl());
			if (!checkTypeFile)
				fileName = FileHandle.ensureUniqueFileName(this.path, fileName);
			this.setFileName(fileName);
			this.downloader.setFileName(fileName);
			this.setFileSize(FileHandle.formatFileSize(fileSize));
			this.setDate(date);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<Downloading> convertDownloadItemToDownloading(List<DownloadItem> items) {
		List<Downloading> uiObjectList = new ArrayList<>();
		if (items == null) {
			System.out.println("List downloadItems la null");
		} else {
			for (DownloadItem item : items) {
				Downloading uiObject = new Downloading(item.url.get(), item.savePath.get());
				uiObject.setUrl(item.url.get());
				uiObject.setPath(item.savePath.get());
				uiObject.setSelected(item.selected.get());
				uiObjectList.add(uiObject);
			}
		}
		return uiObjectList;
	}

	public static void handleListDownload(MainUI mainUI, List<Downloading> downloadFiles) {
		if (!downloadFiles.isEmpty()) {
			for (Downloading i : downloadFiles) {
				if (!i.isSelected()) {
					new Thread(() -> {
						i.updateInfor();
						DownloadWaiting downloadWaiting = new DownloadWaiting(i.getUrl(), i.getFileSize(), i.getPath(),
								"N/A", i.getFileName());
						DownloadWaiting.addWaiting(downloadWaiting);
						mainUI.addDataToMainTable();
					}).start();
				} else if (Downloading.getCountDownloading() < Downloading.getMaxDownloading()) {
					i.setStatus("Đang tải");
					Downloading.incrementCountDownloading();
					new Thread(() -> {
						i.updateInfor();
						mainUI.listFileDownloadingGlobal.add(i);
						mainUI.addDataToMainTable();
						i.start();
						Downloading.decrementCountDownloading();
					}).start();
				} else {
					new Thread(() -> {
						System.out.println("after");
						i.updateInfor();
						DownloadWaiting downloadWaiting = new DownloadWaiting(i.getUrl(), i.getFileSize(), i.getPath(),
								"N/A", i.getFileName());
						DownloadWaiting.addWaitingWithDateCurrent(downloadWaiting);
						mainUI.addDataToMainTable();
					}).start();

				}
			}
		}
	}

	public static synchronized int getMaxDownloading() {
		synchronized (Downloading.class) {
			return Downloading.maxDownloading;
		}

	}

	public static synchronized void setMaxDownloading(int max) {
		synchronized (Downloading.class) {
			Downloading.maxDownloading = max;
		}
	}

	public static synchronized int getCountDownloading() {
		return Downloading.countDownloading;
	}

	public static synchronized void incrementCountDownloading() {
		synchronized (Downloading.class) {
			++Downloading.countDownloading;
		}
	}

	public static synchronized void decrementCountDownloading() {
		synchronized (Downloading.class) {
			--Downloading.countDownloading;
		}
	}

	public boolean isSaveToTxt() {
		return isSaveToTxt;
	}

	public void setSaveToTxt(boolean isSaveToTxt) {
		this.isSaveToTxt = isSaveToTxt;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getFileSize() {
		return fileSize;
	}

	public void setFileSize(String fileSize) {
		this.fileSize = fileSize;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isDownloading() {
		return this.downloader.getCompletedFlag();
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

}
