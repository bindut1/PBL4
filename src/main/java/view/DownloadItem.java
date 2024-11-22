package view;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DownloadItem extends RecursiveTreeObject<DownloadItem> {
	public StringProperty url;
	public StringProperty savePath;
	public BooleanProperty selected;

	public DownloadItem(String url, String savePath) {
		this.url = new SimpleStringProperty(url);
		this.savePath = new SimpleStringProperty(savePath);
		this.selected = new SimpleBooleanProperty(true);
	}
}
