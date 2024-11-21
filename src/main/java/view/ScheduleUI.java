package view;

import com.jfoenix.controls.*;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.time.LocalDate;
import java.util.stream.IntStream;

public class ScheduleUI extends Stage {
	private double xOffset = 0;
	private double yOffset = 0;

	public ScheduleUI(Stage owner) {
		initOwner(owner);
		initModality(Modality.APPLICATION_MODAL);
		initStyle(StageStyle.TRANSPARENT);

		VBox mainContainer = new VBox(0);
		mainContainer.getStyleClass().add("dialog-container");
		mainContainer.setPadding(new Insets(0));
		setWidth(300);
		setHeight(200);

		JFXToolbar header = new JFXToolbar();
		header.getStyleClass().add("dialog-header");

		Label titleLabel = new Label("Lập lịch tải");
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

		DatePicker datePicker = new DatePicker(LocalDate.now());
		datePicker.setPromptText("Chọn ngày");
		datePicker.getStyleClass().add("date-picker");

		HBox datePickerBox = new HBox(datePicker);
		datePickerBox.setAlignment(Pos.CENTER);

		HBox timePickerBox = new HBox(5);
		timePickerBox.setAlignment(Pos.CENTER);
		timePickerBox.getStyleClass().add("time-picker");

		ComboBox<Integer> hourComboBox = new ComboBox<>();
		hourComboBox.getItems().addAll(IntStream.range(0, 24).boxed().toList());
		hourComboBox.setValue(12);
		hourComboBox.getStyleClass().add("combo-box");

		ComboBox<Integer> minuteComboBox = new ComboBox<>();
		minuteComboBox.getItems().addAll(IntStream.range(0, 60).boxed().toList());
		minuteComboBox.setValue(0);
		minuteComboBox.getStyleClass().add("combo-box");

		Label colonLabel = new Label(":");

		timePickerBox.getChildren().addAll(hourComboBox, colonLabel, minuteComboBox);

		HBox buttonBox = new HBox(10);
		buttonBox.setAlignment(Pos.CENTER_RIGHT);

		JFXButton cancelButton = new JFXButton("Hủy");
		cancelButton.getStyleClass().add("cancel-button");

		JFXButton okButton = new JFXButton("OK");
		okButton.getStyleClass().add("ok-button");

		buttonBox.getChildren().addAll(cancelButton, okButton);

		mainContainer.getChildren().addAll(header, datePickerBox, timePickerBox, buttonBox);

		cancelButton.setOnAction(e -> close());
		okButton.setOnAction(e -> {
			LocalDate selectedDate = datePicker.getValue();
			Integer selectedHour = hourComboBox.getValue();
			Integer selectedMinute = minuteComboBox.getValue();

			System.out.printf("Đã lên lịch tải vào ngày %s lúc %02d:%02d%n", selectedDate, selectedHour,
					selectedMinute);

			close();
		});

		Scene scene = new Scene(mainContainer);
		scene.setFill(null);
		scene.getStylesheets().add(getClass().getResource("/view/style1.css").toExternalForm());
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
}
