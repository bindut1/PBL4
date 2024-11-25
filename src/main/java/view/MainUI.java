package view;

import utilUI.*;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jfoenix.controls.*;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import util.FileHandle;
import util.TimeHandle;
import javafx.application.Platform;
import java.lang.Thread;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainUI extends Application {
	private double xOffset = 0;
	private double yOffset = 0;
	private BorderPane root;
	private Stage primaryStage;

	public static List<UIObjectGeneral> listFileDownloadingGlobal = new ArrayList<>();
	public List<String> listFileCompleted = FileHandle.readFileFromTxt("CompletedFileTracking.txt");

	public DownloadUI objDownLoadUI;
	private TableView<MainTableItem> table;
	private TreeView<String> treeView;
	public Map<UIObjectGeneral, ProgressUI> progressUIMap = new HashMap<>();

	@Override
	public void start(Stage primaryStage) {
		objWaiting.getListWaiting();
		this.primaryStage = primaryStage;
		this.treeView = new TreeView<>();
		root = new BorderPane();
		root.getStyleClass().add("main-pane");
		JFXToolbar menuToolbar = new JFXToolbar();
		menuToolbar.getStyleClass().add("menu-toolbar");
		Label titleLabel = new Label("Internet Download Manager");
		titleLabel.getStyleClass().add("title-label");
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		JFXButton minimizeBtn = createIconButton(MaterialDesignIcon.WINDOW_MINIMIZE, "minimize-button");
		JFXButton maximizeBtn = createIconButton(MaterialDesignIcon.WINDOW_MAXIMIZE, "maximize-button");
		JFXButton closeBtn = createIconButton(MaterialDesignIcon.WINDOW_CLOSE, "close-button");
		HBox windowControls = new HBox(5, minimizeBtn, maximizeBtn, closeBtn);
		menuToolbar.setLeft(titleLabel);
		menuToolbar.setRight(windowControls);
		menuToolbar.setOnMousePressed(event -> {
			xOffset = event.getSceneX();
			yOffset = event.getSceneY();
		});
		menuToolbar.setOnMouseDragged(event -> {
			primaryStage.setX(event.getScreenX() - xOffset);
			primaryStage.setY(event.getScreenY() - yOffset);
		});
		VBox toolBarContainer = new VBox(10);
		toolBarContainer.getStyleClass().add("toolbar-container");
		HBox buttonBox = new HBox(10);
		buttonBox.getStyleClass().add("button-box");
		buttonBox.setAlignment(Pos.CENTER);
		double buttonWidth = 250;
		double buttonHeight = 50;
		JFXButton btnAddPath = createActionButton(MaterialDesignIcon.DOWNLOAD, "THỰC HIỆN TẢI", "18");
		btnAddPath.setPrefSize(1000, buttonHeight);
		btnAddPath.setStyle("-fx-font-size: 15px;");
		btnAddPath.setOnAction(e -> {
			if (objDownLoadUI == null) {
				objDownLoadUI = new DownloadUI(primaryStage, this);
			}
			objDownLoadUI.showAndWait();
			addDataToMainTable();
		});
		JFXButton btnResume = createActionButton(MaterialDesignIcon.PLAY, "Tiếp tục", "16");
		btnResume.setPrefSize(buttonWidth, buttonHeight);
		btnResume.setOnAction(e -> {
			ObservableList<MainTableItem> selectedItems = FXCollections
					.observableArrayList(table.getSelectionModel().getSelectedItems());
			resumeHandle(selectedItems);
		});
		JFXButton btnPause = createActionButton(MaterialDesignIcon.PAUSE, "Dừng", "16");
		btnPause.setPrefSize(buttonWidth, buttonHeight);
		btnPause.setOnAction(e -> {
			ObservableList<MainTableItem> selectedItems = FXCollections
					.observableArrayList(table.getSelectionModel().getSelectedItems());
			pauseHandle(selectedItems);
		});
		JFXButton btnDelete = createActionButton(MaterialDesignIcon.DELETE, "Xóa", "16");
		btnDelete.setPrefSize(buttonWidth, buttonHeight);
		btnDelete.setOnAction(e -> {
			// dang loi cai xoa file dang tai nhung van ghi vo file txt, completedFlag bi
			// loi nao do(chua fix)
			// hien tai chua truc tiep xoa file trong path
			ObservableList<MainTableItem> selectedItems = FXCollections
					.observableArrayList(table.getSelectionModel().getSelectedItems());
			deletedHandle(selectedItems);
		});
		JFXButton btnSchedule = createActionButton(MaterialDesignIcon.CLOCK, "Lập lịch", "16");
		btnSchedule.setPrefSize(buttonWidth, buttonHeight);
		btnSchedule.setOnAction(e -> {handleSchedule();	});
		
		buttonBox.getChildren().addAll(btnAddPath);
		HBox footerButtonBox = new HBox(10);
		footerButtonBox.getStyleClass().add("footer-button-box");
		footerButtonBox.setAlignment(Pos.CENTER);
		footerButtonBox.setPadding(new Insets(10));
		footerButtonBox.getChildren().addAll(btnPause, btnResume, btnDelete, btnSchedule);
		toolBarContainer.getChildren().add(buttonBox);
		root.setBottom(footerButtonBox);
		this.treeView = initializeTreeView();
		this.table = initializeTable();
		loadTable("Đã tải");
		setupTableRowListener();
		VBox topContainer = new VBox(menuToolbar, toolBarContainer);
		root.setTop(topContainer);
		root.setLeft(treeView);
		root.setCenter(table);
		Scene scene = new Scene(root, 1000, 600);
		scene.getStylesheets().add(getClass().getResource("/utilUI/style.css").toExternalForm());
		primaryStage.initStyle(StageStyle.UNDECORATED);
		primaryStage.setScene(scene);
		primaryStage.show();
		closeBtn.setOnAction(e -> primaryStage.close());
		minimizeBtn.setOnAction(e -> primaryStage.setIconified(true));
		maximizeBtn.setOnAction(e -> {
			if (primaryStage.isMaximized()) {
				primaryStage.setMaximized(false);
			} else {
				primaryStage.setMaximized(true);
			}
		});
		// Xu ly cap nhat tien do lien tuc
		Timeline progressUpdateTimeline = new Timeline(new KeyFrame(Duration.seconds(1.0), event -> {
			listFileDownloadingGlobal.forEach(info -> {
				ProgressUI objProgressUI = progressUIMap.computeIfAbsent(info, k -> new ProgressUI(primaryStage));
				info.updateProgressUI(objProgressUI);
				if (!info.downloader.getCompletedFlag()) {
					int progress = (int) (info.downloader.getProgress() * 100);
					String status = "Đang tải (" + progress + "%)";
					updateTableRow(info.getFileName(), info.getFileSize(), status);
				} else {
					if (!info.isSaveToTxt()) {
						String time = String.valueOf(
								TimeHandle.formatTime((System.currentTimeMillis() - info.downloader.getStartTime())));
						info.setStatus("Đã tải");
						FileHandle.saveFileCompletedToTxt(info.getFileName(), info.getFileSize(), info.getStatus(),
								info.getDate(), time, info.getPath());
						info.setSaveToTxt(true);
						updateTableRow(info.getFileName(), info.getFileSize(), "Đã tải");
						addDataToMainTable();
					}
				}
			});
		}));
		progressUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
		progressUpdateTimeline.play();
		// xử lí waiting
		Thread handelWaiting = new Thread(() -> {
			while (true) {
				try {
					objWaiting.handelWaiting();
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		});
		handelWaiting.setDaemon(true);
		handelWaiting.start();
		primaryStage.setOnCloseRequest(event -> {
			if (progressUpdateTimeline!=null)
				progressUpdateTimeline.stop();
			handleShutdown();
		});
	}

	private JFXButton createActionButton(MaterialDesignIcon icon, String text, String size) {
		JFXButton button = new JFXButton(text);
		MaterialDesignIconView iconView = new MaterialDesignIconView(icon);
		iconView.setSize(size);
		button.setGraphic(iconView);
		button.getStyleClass().add("action-button");
		return button;
	}

	private JFXButton createIconButton(MaterialDesignIcon icon, String styleClass) {
		JFXButton button = new JFXButton();
		MaterialDesignIconView iconView = new MaterialDesignIconView(icon);
		iconView.setSize("16");
		button.setGraphic(iconView);
		button.getStyleClass().addAll("icon-button", styleClass);
		return button;
	}

	private TreeItem<String> createTreeItem(String text, MaterialDesignIcon icon) {
		TreeItem<String> item = new TreeItem<>(text);
		MaterialDesignIconView iconView = new MaterialDesignIconView(icon);
		iconView.setSize("18");
		item.setGraphic(iconView);
		return item;
	}

	public void addDataToMainTable() {
		String selectedCategory = treeView != null ? treeView.getSelectionModel().getSelectedItem().getValue()
				: "Đã tải";
		if (table == null) {
			this.table = initializeTable();
		}
		table.getItems().clear();
		if (selectedCategory.equals("Đang tải") && listFileDownloadingGlobal != null) {
			loadTable("Đang tải");
		} else if (selectedCategory.equals("Chờ tải")) {
			loadTable("Chờ tải");
		} else if (selectedCategory.equals("Đã tải")) {
			loadTable("Đã tải");
		}
	}

	public void loadTable(String category) {
		if (table == null) {
			this.table = initializeTable();
			setupTableRowListener();
		}
		switch (category) {
		case "Đang tải":
			if (listFileDownloadingGlobal != null) {
				for (UIObjectGeneral i : listFileDownloadingGlobal) {
					if (!i.downloader.getCompletedFlag()) {
						table.getItems().add(
								new MainTableItem(i.getFileName(), i.getFileSize(), i.getStatus(), i.getDate(), "N/A"));
					}
				}
			}
			break;

		case "Chờ tải":
			List<objWaiting> waitings = objWaiting.getListWaiting();
			for (objWaiting objWaiting : waitings) {
				table.getItems().add(new MainTableItem(objWaiting.getFileName(), objWaiting.getFilesize(), "Chờ tải",
						objWaiting.getTime(), "N/A"));
			}
			break;

		case "Đã tải":
			listFileCompleted = FileHandle.readFileFromTxt("CompletedFileTracking.txt");
			if (listFileCompleted != null) {
				for (String i : listFileCompleted) {
					String[] parts = i.split(",");
					table.getItems().add(new MainTableItem(parts[0], parts[1], parts[2], parts[3], parts[4]));
				}
			}
			break;

		default:
			System.out.println("Không xác định được category");
		}
	}

	private TreeView<String> initializeTreeView() {
		TreeView<String> treeView = new TreeView<>();
		TreeItem<String> rootItem = new TreeItem<>("Danh mục");
		rootItem.getChildren().addAll(createTreeItem("Đã tải", MaterialDesignIcon.CHECK_CIRCLE),
				createTreeItem("Đang tải", MaterialDesignIcon.DOWNLOAD),
				createTreeItem("Chờ tải", MaterialDesignIcon.TIMER));
		treeView.setRoot(rootItem);
		treeView.setShowRoot(false);
		treeView.getStyleClass().add("custom-tree-view");
		treeView.getSelectionModel().select(rootItem.getChildren().get(0));
		treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				addDataToMainTable();
			}
		});
		return treeView;
	}

	private TableView<MainTableItem> initializeTable() {
		TableView<MainTableItem> table = new TableView<>();
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		TableColumn<MainTableItem, String> nameCol = new TableColumn<>("Tên tệp");
		TableColumn<MainTableItem, String> sizeCol = new TableColumn<>("Kích thước");
		TableColumn<MainTableItem, String> statusCol = new TableColumn<>("Trạng thái");
		TableColumn<MainTableItem, String> dateCol = new TableColumn<>("Ngày tải");
		TableColumn<MainTableItem, String> timeCol = new TableColumn<>("Thời gian tải");
		
		nameCol.setPrefWidth(300);
		sizeCol.setPrefWidth(100);
		statusCol.setPrefWidth(100);
		dateCol.setPrefWidth(150);
		timeCol.setPrefWidth(100);

		nameCol.setCellValueFactory(new PropertyValueFactory<>("url"));
		sizeCol.setCellValueFactory(new PropertyValueFactory<>("size"));
		statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
		dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
		timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));

		table.getColumns().addAll(nameCol, sizeCol, statusCol, dateCol, timeCol);
		table.getStyleClass().add("custom-table-view");
		return table;
	}
	
	
	private void handleSchedule() {
		String selectedCategory = treeView != null ? treeView.getSelectionModel().getSelectedItem().getValue()
				: "Đã tải";
		ObservableList<MainTableItem> selectedItems = FXCollections
				.observableArrayList(table.getSelectionModel().getSelectedItems());
		if (selectedCategory.equals("Chờ tải") && !selectedItems.isEmpty()) {
			ScheduleUI objScheduleUI = new ScheduleUI(primaryStage);
			objScheduleUI.showAndWait();
			String timeString = objScheduleUI.getTime();
			List<objWaiting> objWaitings = objWaiting.getListWaiting();
			if (!timeString.equals("")) {
				for (MainTableItem item : selectedItems) {
					for (objWaiting waiting : objWaitings) {
						String selectedFileName = String.valueOf(item.urlProperty().getValue());
						if (selectedFileName.equals(waiting.getFileName())) {
							waiting.updateTime(timeString);
						}
					}
				}
				addDataToMainTable();
			}
		}
	}
	

	private void handleShutdown() {
		listFileDownloadingGlobal.forEach(info -> {
			if (info.downloaderNotNull())
				info.downloader.cancel();
			System.exit(0);
		});
	}

	// ham xu ly khi double click
	private void setupTableRowListener() {
		table.setRowFactory(tv -> {
			TableRow<MainTableItem> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (!row.isEmpty())) {
					MainTableItem selectedItem = row.getItem();
					String selectedFileName = String.valueOf(selectedItem.urlProperty().getValue());
					String selectedFileSize = String.valueOf(selectedItem.sizeProperty().getValue());
					String selectedStatus = String.valueOf(selectedItem.statusProperty().getValue());

					UIObjectGeneral fileSelected = null;
					for (UIObjectGeneral info : listFileDownloadingGlobal) {
						if (info.getFileName().equals(selectedFileName)
								&& info.getFileSize().equals(selectedFileSize)) {
							fileSelected = info;
							break;
						}
					}

					if (fileSelected != null) {
						final UIObjectGeneral finalFileSelected = fileSelected;
						ProgressUI objProgressUI = progressUIMap.computeIfAbsent(finalFileSelected, k -> {
							return new ProgressUI(primaryStage);
						});
						objProgressUI.showAndWait();
					} else {
						String filePath = "";
						if (selectedStatus.trim().equals("Đã tải")) {
							if (listFileCompleted != null) {
								for (String completedFile : listFileCompleted) {
									String[] parts = completedFile.split(",");
									if (parts[0].equals(selectedFileName)) {
										filePath = parts[5];
										break;
									}
								}
							}
						} else if (selectedStatus.trim().equals("Chờ tải")) {
							List<objWaiting> waitings = objWaiting.getListWaiting();
							for (objWaiting waiting : waitings) {
								if (waiting.getFileName().equals(selectedFileName)) {
									filePath = waiting.getSavePath();
									break;
								}
							}
						}

						if (!filePath.isEmpty()) {
							try {
								File file = new File(filePath.trim());
								if (file.exists()) {
									Desktop.getDesktop().open(file);
								} else {
									System.out.println("File does not exist!");
								}
							} catch (IOException e) {
								e.printStackTrace();
								System.out.println("loi khi co gang mo file");
							}
						} else {
							System.out.println("No file path found!");
						}
					}
				}
			});
			return row;
		});
	}

	// ham de cap nhat lien tuc % cua file dang tai
	private void updateTableRow(String fileName, String fileSize, String status) {
		ObservableList<MainTableItem> items = table.getItems();
		for (MainTableItem item : items) {
			if (item.urlProperty().getValue().equals(fileName) && item.sizeProperty().getValue().equals(fileSize)) {
				item.setStatus(status);
				table.refresh();
				break;
			}
		}
	}

	public void checkFileSelected(ObservableList<MainTableItem> selectedItems, String txt) {
		if (selectedItems.isEmpty()) {
			System.out.println("Chon it nhat 1 file de " + txt);
			return;
		}

		boolean checkInvalidFile = false;
		for (MainTableItem item : selectedItems) {
			if (!item.statusProperty().getValue().contains("Đang tải")) {
				checkInvalidFile = true;
				break;
			}
		}

		if (checkInvalidFile) {
			System.out.println("Chi co the " + txt + " cac file o trang thai Dang tai");
			return;
		}
	}

	public void pauseHandle(ObservableList<MainTableItem> selectedItems) {
		checkFileSelected(selectedItems, "tam dung");
		for (MainTableItem item : selectedItems) {
			String fileName = item.urlProperty().getValue();
			for (UIObjectGeneral info : listFileDownloadingGlobal) {
				if (info.getFileName().equals(fileName) && info.downloaderNotNull()
						&& info.downloader.getRunningFlag()) {
					info.downloader.pause();
				}
			}
		}
	}

	public void resumeHandle(ObservableList<MainTableItem> selectedItems) {
		checkFileSelected(selectedItems, "tiep tuc");
		for (MainTableItem item : selectedItems) {
			String fileName = item.urlProperty().getValue();
			for (UIObjectGeneral info : listFileDownloadingGlobal) {
				if (info.getFileName().equals(fileName) && info.downloaderNotNull()
						&& !info.downloader.getRunningFlag()) {
					info.downloader.resume();
				}
			}
		}
	}

	public void deletedHandle(ObservableList<MainTableItem> selectedItems) {
		if (selectedItems.isEmpty()) {
			return;
		}

		for (MainTableItem item : selectedItems) {
			String fileName = item.urlProperty().getValue();
			String status = item.statusProperty().getValue();

			if (status.contains("Đang tải")) {
				listFileDownloadingGlobal.stream().filter(info -> info.getFileName().equals(fileName)
						&& info.downloaderNotNull() && info.downloader.getRunningFlag())
						.forEach(info -> info.downloader.cancel());
			} else if (status.contains("Đã tải")) {
				if (listFileCompleted != null && !listFileCompleted.isEmpty()) {
					listFileCompleted.stream().filter(info -> info.split(",")[0].equals(fileName)).forEach(info -> {
						FileHandle.deleteLineFromTxtFile("CompletedFileTracking.txt", info);
					});
				}
			} else {
						objWaiting.deleteWaiting(fileName);
			}
			table.getItems().remove(item);
		}
		addDataToMainTable();
	}
	public static void main(String[] args) {
		launch(args);
	}
}