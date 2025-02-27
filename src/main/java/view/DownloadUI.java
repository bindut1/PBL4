package view;

import utilUI.*;
import downloadUI.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.jfoenix.controls.*;
import com.jfoenix.controls.cells.editors.base.JFXTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;

import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DownloadUI extends Stage {

	private double xOffset = 0;
	private double yOffset = 0;
	private final ObservableList<DownloadItem> downloadItems = FXCollections.observableArrayList();
	JFXTreeTableView<DownloadItem> downloadTable;

	public MainUI mainUI;
	public ProgressUI objProgressUI;

	private String defaultSavePath = "D:\\PBL4\\SAVE";
	Timeline progressUpdateTimeline;

	public DownloadUI(Stage owner, MainUI mainUI) {
		this.mainUI = mainUI;
		this.objProgressUI = new ProgressUI(owner);

		initOwner(owner);
		initModality(Modality.APPLICATION_MODAL);
		initStyle(StageStyle.TRANSPARENT);

		VBox mainContainer = new VBox(10);
		mainContainer.getStyleClass().add("dialog-container");
		mainContainer.setPadding(new Insets(0));
		setWidth(600);
		setHeight(400);

		JFXToolbar header = new JFXToolbar();
		header.getStyleClass().add("dialog-header");

		Label titleLabel = new Label("Thêm tập tin tải xuống");
		titleLabel.getStyleClass().add("dialog-title");

		JFXButton closeButton = createIconButton(MaterialDesignIcon.CLOSE, "dialog-close-button");
		closeButton.setOnAction(e -> close());

		header.setLeft(titleLabel);
		header.setRight(closeButton);

		header.setOnMousePressed(event -> {
			xOffset = event.getSceneX();
			yOffset = event.getSceneY();
		});

		header.setOnMouseDragged(event -> {
			setX(event.getScreenX() - xOffset);
			setY(event.getScreenY() - yOffset);
		});

		HBox inputBox = new HBox(10);
		inputBox.setAlignment(Pos.CENTER_LEFT);
		inputBox.setPadding(new Insets(10));

		JFXTextField urlInput = new JFXTextField();
		urlInput.setPromptText("Nhập URL tải xuống");
		urlInput.getStyleClass().add("url-input");
		HBox.setHgrow(urlInput, Priority.ALWAYS);

		JFXButton addUrlButton = new JFXButton("Thêm URL");
		addUrlButton.getStyleClass().add("add-url-button");
		MaterialDesignIconView addIcon = new MaterialDesignIconView(MaterialDesignIcon.PLUS);
		addIcon.setSize("18");
		addUrlButton.setGraphic(addIcon);
		addUrlButton.setOnAction(e -> {
		    String url = urlInput.getText().trim();
		    if (!url.isEmpty() && validateInput(url)) {
		        boolean alreadyExists = downloadItems.stream()
		                .anyMatch(item -> item.url.get().equals(url));
		        if (alreadyExists) {
		            AlertUI alertUI = new AlertUI(owner, "Thông báo", "URL đã tồn tại trong danh sách tải!");
		            alertUI.showAndWait();
		        } else {
		            downloadItems.add(new DownloadItem(url, defaultSavePath));
		            urlInput.clear();
		        }
		    } else {
		        AlertUI alertUI = new AlertUI(owner, "Thông báo", "Đầu vào không đúng định dạng!");
		        alertUI.showAndWait();
		    }
		});


		JFXButton addTorrentButton = new JFXButton("Thêm Path");
		addTorrentButton.getStyleClass().add("add-url-button");
		MaterialDesignIconView torrentIcon = new MaterialDesignIconView(MaterialDesignIcon.FILE);
		torrentIcon.setSize("18");
		addTorrentButton.setGraphic(torrentIcon);

		addTorrentButton.setOnAction(e -> {
			boolean torrentExistsTable = downloadItems.stream()
		            .anyMatch(item -> item.url.get().endsWith(".torrent")); 
		    
		    if (mainUI.checkExistTorrentFile || torrentExistsTable) {
		        AlertUI alertUI = new AlertUI(owner, "Thông báo", "Hệ thống không hỗ trợ tải song song các file torrent. "
		                + "Bạn không thể thêm file torrent mới!");
		        alertUI.showAndWait();
		        return;
		    }

		    // Nếu không có file torrent, cho phép chọn file torrent
		    FileChooser fileChooser = new FileChooser();
		    fileChooser.setTitle("Chọn tập tin Torrent");
		    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Torrent Files", "*.torrent"));
		    var file = fileChooser.showOpenDialog(this);
		    if (file != null) {
		    	mainUI.checkExistTorrentFile = true;
		        downloadItems.add(new DownloadItem(file.getAbsolutePath(), defaultSavePath));
		    }
		});




		inputBox.getChildren().addAll(urlInput, addUrlButton, addTorrentButton);

		downloadTable = new JFXTreeTableView<>();
		TreeItem<DownloadItem> root = new RecursiveTreeItem<>(downloadItems, RecursiveTreeObject::getChildren);
		downloadTable.setRoot(root);
		downloadTable.setShowRoot(false);
		downloadTable.setEditable(true);
		downloadTable.getStyleClass().add("download-table");

		JFXTreeTableColumn<DownloadItem, Boolean> checkboxColumn = new JFXTreeTableColumn<>("");
		checkboxColumn.setPrefWidth(40);
		checkboxColumn.setCellValueFactory(param -> param.getValue().getValue().selected);
		checkboxColumn.setCellFactory(p -> new JFXTreeTableCell<>() {
			private final JFXCheckBox checkBox = new JFXCheckBox();

			@Override
			protected void updateItem(Boolean item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					DownloadItem downloadItem = getTreeTableRow().getItem();
					if (downloadItem != null) {
						checkBox.selectedProperty().bindBidirectional(downloadItem.selected);
					}
					setGraphic(checkBox);
				}
			}
		});

		JFXTreeTableColumn<DownloadItem, String> filenameColumn = new JFXTreeTableColumn<>("Liên kết");
		filenameColumn.setPrefWidth(257);
		filenameColumn.setCellValueFactory(param -> param.getValue().getValue().url);

		JFXTreeTableColumn<DownloadItem, String> savePathColumn = new JFXTreeTableColumn<>("Đường dẫn lưu");
		savePathColumn.setPrefWidth(257);
		savePathColumn.setCellValueFactory(param -> param.getValue().getValue().savePath);
		savePathColumn.setCellFactory(p -> new JFXTreeTableCell<>() {
			private final HBox container = new HBox(5);
			private final JFXTextField pathField = new JFXTextField();
			private final JFXButton chooseFolderButton = createIconButton(MaterialDesignIcon.FOLDER,
					"choose-folder-button");

			{
				pathField.setEditable(false);
				HBox.setHgrow(pathField, Priority.ALWAYS);
				container.getChildren().addAll(pathField, chooseFolderButton);

				chooseFolderButton.setOnAction(event -> {
					DirectoryChooser directoryChooser = new DirectoryChooser();
					directoryChooser.setTitle("Chọn thư mục lưu");
					directoryChooser.setInitialDirectory(new java.io.File(defaultSavePath));

					java.io.File selectedDirectory = directoryChooser.showDialog(getScene().getWindow());
					if (selectedDirectory != null) {
						DownloadItem item = getTreeTableRow().getItem();
						if (item != null) {
							item.savePath.set(selectedDirectory.getAbsolutePath());
						}
					}
				});
			}

			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					pathField.setText(item);
					setGraphic(container);
				}
			}
		});

		JFXTreeTableColumn<DownloadItem, String> deleteColumn = new JFXTreeTableColumn<>("");
		deleteColumn.setPrefWidth(40);
		deleteColumn.setCellFactory(p -> new JFXTreeTableCell<>() {
			private final JFXButton deleteButton = createIconButton(MaterialDesignIcon.DELETE, "delete-button");

			{
				deleteButton.setOnAction(event -> {
					DownloadItem downloadItem = getTreeTableRow().getItem();
					if (downloadItem != null) {
						downloadItems.remove(downloadItem);
					}
				});
			}

			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(empty ? null : deleteButton);
			}
		});

		downloadTable.getColumns().addAll(checkboxColumn, filenameColumn, savePathColumn, deleteColumn);

		HBox buttonBox = new HBox(10);
		buttonBox.setAlignment(Pos.CENTER_RIGHT);
		buttonBox.setPadding(new Insets(10));

		JFXButton cancelButton = new JFXButton("Hủy");
		cancelButton.getStyleClass().add("cancel-button");
		cancelButton.setOnAction(e -> close());

		JFXButton downloadButton = new JFXButton("Bắt đầu tải");
		downloadButton.getStyleClass().add("download-button");
		
		downloadButton.setDisable(downloadItems.isEmpty());
		downloadItems.addListener((ListChangeListener<? super DownloadItem>) change -> {
		    while (change.next()) {
		        if (change.wasAdded() || change.wasRemoved()) {
		            downloadButton.setDisable(downloadItems.isEmpty());
		        }
		    }
		});

		downloadButton.setOnAction(e -> {
			handleDownload(owner);
			Platform.runLater(() -> {
				close();
			});
		});

		buttonBox.getChildren().addAll(cancelButton, downloadButton);

		mainContainer.getChildren().addAll(header, inputBox, downloadTable, buttonBox);

		Scene scene = new Scene(mainContainer);
		scene.setFill(null);
		scene.getStylesheets().add(getClass().getResource("/utilUI/style1.css").toExternalForm());
		setScene(scene);

		Platform.runLater(() -> {
			if (owner != null) {
				setX(owner.getX() + (owner.getWidth() - getWidth()) / 2);
				setY(owner.getY() + (owner.getHeight() - getHeight()) / 2);
			}
		});
	}

	private JFXButton createIconButton(MaterialDesignIcon icon, String styleClass) {
		JFXButton button = new JFXButton();
		MaterialDesignIconView iconView = new MaterialDesignIconView(icon);
		iconView.setSize("16");
		button.setGraphic(iconView);
		button.getStyleClass().add(styleClass);
		return button;
	}

	private boolean validateInput(String input) {
		String urlRegex = "^(http?|https)://[^\\s/$.?#].[^\\s]*$";
		return Pattern.matches(urlRegex, input);
	}

	public void handleDownload(Stage owner) {
		new Thread(() -> {
			try {
				List<Downloading> downloadFiles = Downloading.convertDownloadItemToDownloading(downloadItems);
				Downloading.handleListDownload(mainUI, downloadFiles);
				Platform.runLater(() -> {
					downloadItems.clear();
					downloadTable.refresh();
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}
}