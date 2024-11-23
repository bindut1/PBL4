package view;

import java.util.ArrayList;
import java.util.List;

import download.AbstractDownloadObject;
import download.DownloadObject;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeView;

public class UIObjectGeneral extends DownloadObject {
	private String url;
	private String path;
	private String fileName;
	private String status;
	private String fileSize;
	private String date;
	private String time;
	private double progress;
	private String textarea;
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

				uiObjectList.add(uiObject);
			}
		}
		return uiObjectList;
	}
}
