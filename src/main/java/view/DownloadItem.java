package view;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DownloadItem extends RecursiveTreeObject<DownloadItem> {
	public StringProperty filename;
	public StringProperty savePath;
	public BooleanProperty selected;

	public DownloadItem(String filename, String savePath) {
		this.filename = new SimpleStringProperty(filename);
		this.savePath = new SimpleStringProperty(savePath);
		this.selected = new SimpleBooleanProperty(true);
	}

}
