package view;

import com.jfoenix.controls.*;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SettingUI extends Stage {

    private double xOffset = 0;
    private double yOffset = 0;

    private int maxFiles = 5;    // Số file tải tối đa mặc định
    private int maxThreads = 5; // Số luồng tải mặc định
    private int trunkSize = 1024; // Dung lượng mỗi trunk mặc định (KB)

    public SettingUI(Stage owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setWidth(400);
        setHeight(250);

        VBox mainContainer = new VBox(0); // Giảm khoảng cách giữa các thành phần chính
        mainContainer.getStyleClass().add("dialog-container");
        mainContainer.setPadding(new Insets(0)); // Giảm padding tổng thể

        JFXToolbar header = new JFXToolbar();
        header.getStyleClass().add("dialog-header");

        Label titleLabel = new Label("Cài đặt");
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

        VBox settingsBox = new VBox(0); // Giảm khoảng cách giữa các dòng
        settingsBox.setPadding(new Insets(5)); // Giảm padding trong hộp
        settingsBox.setAlignment(Pos.CENTER_LEFT);

        // Cài đặt số file tải tối đa
        HBox maxFilesBox = createSettingControl("Số file tải tối đa", maxFiles, 1, 10, value -> maxFiles = value);

        // Cài đặt số luồng tải
        HBox maxThreadsBox = createSettingControl("Số luồng tải", maxThreads, 1, 10, value -> maxThreads = value);

        // Cài đặt dung lượng mỗi trunk
        HBox trunkSizeBox = createTrunkSettingControl();

        settingsBox.getChildren().addAll(maxFilesBox, maxThreadsBox, trunkSizeBox);
        VBox.setVgrow(settingsBox, Priority.ALWAYS); // Phân bổ không gian linh hoạt

        HBox buttonBox = new HBox(5);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10));

        JFXButton saveButton = new JFXButton("Lưu");
        saveButton.getStyleClass().add("save-button");
        saveButton.setOnAction(e -> {
            System.out.println("Số file tải tối đa: " + maxFiles);
            System.out.println("Số luồng tải: " + maxThreads);
            System.out.println("Dung lượng mỗi trunk: " + trunkSize + " KB");
            close();
        });

        JFXButton cancelButton = new JFXButton("Hủy");
        cancelButton.getStyleClass().add("cancel-button");
        cancelButton.setOnAction(e -> close());

        buttonBox.getChildren().addAll(cancelButton, saveButton);

        mainContainer.getChildren().addAll(header, settingsBox, buttonBox);

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

    private HBox createSettingControl(String labelText, int initialValue, int min, int max, ValueUpdater updater) {
        Label label = new Label(labelText);
        label.setPrefWidth(150);

        Spinner<Integer> spinner = new Spinner<>(min, max, initialValue);
        spinner.setPrefWidth(100);
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> updater.update(newValue));

        HBox box = new HBox(10, label, spinner);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox createTrunkSettingControl() {
        Label label = new Label("Dung lượng mỗi trunk (KB):");
        label.setPrefWidth(150);

        TextField trunkField = new TextField(String.valueOf(trunkSize));
        trunkField.setPrefWidth(100);
        trunkField.textProperty().addListener((obs, oldValue, newValue) -> {
            try {
                int value = Integer.parseInt(newValue);
                if (value >= 1) {
                    trunkSize = value;
                } else {
                    trunkField.setText(oldValue);
                }
            } catch (NumberFormatException e) {
                trunkField.setText(oldValue);
            }
        });

        HBox box = new HBox(10, label, trunkField);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private JFXButton createIconButton(MaterialDesignIcon icon, String styleClass) {
        JFXButton button = new JFXButton();
        MaterialDesignIconView iconView = new MaterialDesignIconView(icon);
        iconView.setSize("16");
        button.setGraphic(iconView);
        button.getStyleClass().add(styleClass);
        return button;
    }

    @FunctionalInterface
    private interface ValueUpdater {
        void update(int value);
    }
}
