package view;

import com.jfoenix.controls.*;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressUI extends Stage {
	private double xOffset = 0;
	private double yOffset = 0;

	private ProgressBar progress;
	private TextArea textarea;

	private boolean autoScroll = true;

	private final JFXToggleButton autoScrollToggle;
	private double lastScrollPosition = 0;

	public ProgressUI(Stage owner) {
		initOwner(owner);
		initModality(Modality.APPLICATION_MODAL);
		initStyle(StageStyle.TRANSPARENT);
		setWidth(600);
		setHeight(400);

		VBox mainContainer = new VBox(0);
		mainContainer.getStyleClass().add("dialog-container");
		mainContainer.setPadding(new Insets(0));

		JFXToolbar header = new JFXToolbar();
		header.getStyleClass().add("dialog-header");

		Label titleLabel = new Label("Tiến độ tải");
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

		progress = new ProgressBar(0);
		progress.setPrefHeight(35);
		progress.setPrefWidth(600);
		progress.setStyle("-fx-accent: #3498db;");

		HBox controlsBox = new HBox(0);
		controlsBox.setAlignment(Pos.CENTER_RIGHT);
		VBox.setMargin(controlsBox, new Insets(-20, 0, -20, 0));

		autoScrollToggle = new JFXToggleButton();
		autoScrollToggle.setSelected(true);
		autoScrollToggle.setText("Tự động cuộn");
		autoScrollToggle.setOnAction(e -> {
			autoScroll = autoScrollToggle.isSelected();
			if (!autoScroll) {
				lastScrollPosition = textarea.getScrollTop();
			}
		});
		controlsBox.getChildren().add(autoScrollToggle);

		textarea = new TextArea();
		textarea.setEditable(false);
		textarea.setPrefHeight(300);
		textarea.setWrapText(true);
		textarea.getStyleClass().add("progress-details");

		textarea.scrollTopProperty().addListener((obs, oldVal, newVal) -> {
			if (!autoScroll) {
				lastScrollPosition = newVal.doubleValue();
			}
		});

		mainContainer.getChildren().addAll(header, progress, controlsBox, textarea);

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

	public void updateProgress(double progressValue) {
		Platform.runLater(() -> {
			progress.setProgress(progressValue);
		});
	}

	public void appendText(String text) {
		Platform.runLater(() -> {
			if (!autoScroll)
				lastScrollPosition = textarea.getScrollTop();
			int caretPosition = textarea.getCaretPosition();
			textarea.appendText(text + "\n");
			textarea.positionCaret(caretPosition);
			if (autoScroll)
				textarea.setScrollTop(Double.MAX_VALUE);
			else
				textarea.setScrollTop(lastScrollPosition);
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
