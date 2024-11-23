package view;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jfoenix.controls.*; // Thư viện JFoenix để tạo giao diện đẹp hơn
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView; // Thư viện biểu tượng Material Design
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
import util.HttpConnection;
import util.TimeHandle;

public class MainUI extends Application {
	private double xOffset = 0;
	private double yOffset = 0;
	private BorderPane root;
	private Stage primaryStage;

	// Thao tac tren 3 list tuong ung voi 3 trang thai
	public static List<UIObjectGeneral> listFileDownloadingGlobal = new ArrayList<>();
	public List<String> listFileWaiting = FileHandle.readFileFromTxt("WaitingFileTracking.txt");
	public List<String> listFileCompleted = FileHandle.readFileFromTxt("CompletedFileTracking.txt");

	public DownloadUI objDownLoadUI;
	private TableView<MainTableItem> table;
	private TreeView<String> treeView;

	public Map<UIObjectGeneral, ProgressUI> progressUIMap = new HashMap<>();

	@Override
	public void start(Stage primaryStage) {
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

		JFXButton btnPause = createActionButton(MaterialDesignIcon.PAUSE, "Dừng", "16");
		btnPause.setPrefSize(buttonWidth, buttonHeight);

		JFXButton btnDelete = createActionButton(MaterialDesignIcon.DELETE, "Xóa", "16");
		btnDelete.setPrefSize(buttonWidth, buttonHeight);

		JFXButton btnSchedule = createActionButton(MaterialDesignIcon.CLOCK, "Lập lịch", "16");
		btnSchedule.setPrefSize(buttonWidth, buttonHeight);
		btnSchedule.setOnAction(e -> {
			ScheduleUI objScheduleUI = new ScheduleUI(primaryStage);
			objScheduleUI.showAndWait();
		});

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
		scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());

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
		Timeline progressUpdateTimeline = new Timeline(new KeyFrame(Duration.seconds(1.5), event -> {
			listFileDownloadingGlobal.forEach(info -> {
				// Cập nhật ProgressUI
				ProgressUI objProgressUI = progressUIMap.computeIfAbsent(info, k -> new ProgressUI(primaryStage));
				info.updateProgressUI(objProgressUI);

				// Cập nhật status trong table
				if (!info.downloader.getCompletedFlag()) {
					int progress = (int) (info.downloader.getProgress() * 100);
					String status = "Đang tải (" + progress + "%)";
					updateTableRow(info.getFileName(), info.getFileSize(), status);
				} else {
					if (!info.isSaveToTxt()) {
						String time = String.valueOf(TimeHandle
								.formatTime((System.currentTimeMillis() - info.downloader.getStartTime())));
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

		primaryStage.setOnCloseRequest(event -> {
			if (progressUpdateTimeline != null) {
				progressUpdateTimeline.stop();
			}
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
//		System.out.println("Status hien tai: " + selectedCategory);
		if (selectedCategory.equals("Đang tải") && listFileDownloadingGlobal != null) {
			loadTable("Đang tải");
		} else if (selectedCategory.equals("Chờ tải") && listFileWaiting != null) {
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
			listFileWaiting = FileHandle.readFileFromTxt("WaitingFileTracking.txt");
			if (listFileWaiting != null) {
				for (String i : listFileWaiting) {
					String[] parts = i.split(",");
					table.getItems().add(new MainTableItem(parts[0], parts[1], "Chờ tải", "N/A", "N/A"));
				}
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

	public void handleShutdown() {
		listFileDownloadingGlobal.forEach(info -> {
			if (info.downloaderNotNull())
				info.downloader.cancel();
			System.exit(0);
		});
	}

	//ham xu ly khi double click
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
	                                //check ten file trung thi + 1 chu ko cho ni sai
	                                if (parts[0].equals(selectedFileName)) {
	                                    filePath = parts[5];
	                                    break;
	                                }
	                            }
	                        }
	                    }
	                    else if (selectedStatus.trim().equals("Chờ tải")) {
	                        for (String waitingFile : listFileWaiting) {
	                            String[] parts = waitingFile.split(",");
	                            if (parts[0].equals(selectedFileName)) {
	                                filePath = parts[2];
	                                break;
	                            }
	                        }
	                    }
	                    
	                    if (!filePath.isEmpty()) {
	                        try {
	                            File file = new File(filePath.trim());
	                            System.out.println("path ne: " + file);
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

	public static void main(String[] args) {
		launch(args);
	}
}