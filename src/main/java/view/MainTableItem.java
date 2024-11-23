package view;

import javafx.beans.property.SimpleStringProperty;

public class MainTableItem {
	private SimpleStringProperty url;
	private SimpleStringProperty size;
	private SimpleStringProperty status;
	private SimpleStringProperty date;
	private SimpleStringProperty time;

	public MainTableItem(String url, String size, String status, String date, String time) {
		this.url = new SimpleStringProperty(url);
		this.size = new SimpleStringProperty(size);
		this.status = new SimpleStringProperty(status);
		this.date = new SimpleStringProperty(date);
		this.time = new SimpleStringProperty(time);
	}

	public SimpleStringProperty urlProperty() {
		return url;
	}

	public SimpleStringProperty sizeProperty() {
		return size;
	}

	public SimpleStringProperty statusProperty() {
		return status;
	}

	public SimpleStringProperty dateProperty() {
		return date;
	}

	public SimpleStringProperty timeProperty() {
		return time;
	}
	
	public void setStatus(String newStatus) {
        this.status.set(newStatus);
    }
}
