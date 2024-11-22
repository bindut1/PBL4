package view;

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
	private double progress;
	private String textarea;
	private boolean selected = false;

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

	public void updateProgressUI(UIObjectGeneral objUIObjectGeneral, ProgressUI progressUI) {
		if (objUIObjectGeneral.downloaderNotNull() && objUIObjectGeneral.downloader.getRunningFlag()) {
			progressUI.updateProgress(objUIObjectGeneral.downloader.getProgress());
			progressUI.appendText(objUIObjectGeneral.downloader.getDetailText());
		}
	}

}
