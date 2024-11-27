package view;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import download.DownloadObject;
import util.FileHandle;

public class UIObjectGeneral extends DownloadObject {
	private String url;
	private String path;
	private String fileName;
	private String status;
	private String fileSize;
	private String date;
	private String time;
	private boolean selected = false;
	private boolean isSaveToTxt = false;

	public boolean isSaveToTxt() {
		return isSaveToTxt;
	}

	public void setSaveToTxt(boolean isSaveToTxt) {
		this.isSaveToTxt = isSaveToTxt;
	}

	public UIObjectGeneral(String url, String path) {
		super(url, path);
	}
	
	public UIObjectGeneral(objWaiting waiting) {
		super(waiting.getUrl(), waiting.getSavePath());
		this.fileName = waiting.getFileName();
		this.date = waiting.getTime();
		this.fileSize = waiting.getFilesize();
		this.path = waiting.getSavePath();
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

	public void updateProgressUI(ProgressUI progressUI) {
		if (this.downloaderNotNull() && this.downloader.getRunningFlag()) {
			progressUI.updateProgress(this.downloader.getProgress());
			progressUI.appendText(this.downloader.getDetailText());
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
			this.setFileName(fileName);
			this.setFileSize(FileHandle.formatFileSize(fileSize));
			this.setDate(date);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static List<UIObjectGeneral> convertDownloadItemToUIObjectGeneral(List<DownloadItem> items) {
		List<UIObjectGeneral> uiObjectList = new ArrayList<>();
		if (items == null) {
			System.out.println("List downloadItems la null");
		} else {
			for (DownloadItem item : items) {
				UIObjectGeneral uiObject = new UIObjectGeneral(item.url.get(), item.savePath.get());
				uiObject.setUrl(item.url.get());
				uiObject.setPath(item.savePath.get());
				uiObject.setSelected(item.selected.get());
				uiObject.updateInfor();
				uiObjectList.add(uiObject);
			}
		}
		return uiObjectList;
	}
}
