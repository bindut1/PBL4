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

	private final ProgressBar overallProgressBar;
	private TextArea progressDetails;

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

		overallProgressBar = new ProgressBar(0);
		overallProgressBar.setPrefHeight(35);
		overallProgressBar.setPrefWidth(600);
		overallProgressBar.setStyle("-fx-accent: #3498db;");

		HBox controlsBox = new HBox(0);
		controlsBox.setAlignment(Pos.CENTER_RIGHT);
		VBox.setMargin(controlsBox, new Insets(-20, 0, -20, 0));

		autoScrollToggle = new JFXToggleButton();
		autoScrollToggle.setSelected(true);
		autoScrollToggle.setText("Tự động cuộn");
		autoScrollToggle.setOnAction(e -> {
			autoScroll = autoScrollToggle.isSelected();
			if (!autoScroll) {
				lastScrollPosition = progressDetails.getScrollTop();
			}
		});
		controlsBox.getChildren().add(autoScrollToggle);

		progressDetails = new TextArea();
		progressDetails.setEditable(false);
		progressDetails.setPrefHeight(300);
		progressDetails.setWrapText(true);
		progressDetails.getStyleClass().add("progress-details");

		progressDetails.scrollTopProperty().addListener((obs, oldVal, newVal) -> {
			if (!autoScroll) {
				lastScrollPosition = newVal.doubleValue();
			}
		});

		mainContainer.getChildren().addAll(header, overallProgressBar, controlsBox, progressDetails);

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

	private void appendTextWithAutoScroll(String text) {
		Platform.runLater(() -> {
			if (!autoScroll)
				lastScrollPosition = progressDetails.getScrollTop();
			int caretPosition = progressDetails.getCaretPosition();
			progressDetails.appendText(text);
			progressDetails.positionCaret(caretPosition);
			if (autoScroll)
				progressDetails.setScrollTop(Double.MAX_VALUE);
			else
				progressDetails.setScrollTop(lastScrollPosition);
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
