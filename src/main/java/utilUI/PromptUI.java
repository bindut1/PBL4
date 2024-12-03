package utilUI;

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
import java.util.concurrent.CompletableFuture;

public class PromptUI extends Stage {
	private double xOffset = 0;
	private double yOffset = 0;
	private boolean result = false;

	public PromptUI(Stage owner, String title, String message) {

		initOwner(owner);
		initModality(Modality.APPLICATION_MODAL);
		initStyle(StageStyle.TRANSPARENT);

		VBox mainContainer = new VBox(10);
		mainContainer.getStyleClass().add("dialog-container");
		mainContainer.setPadding(new Insets(0));
		setWidth(300);
		setHeight(150);

		JFXToolbar header = new JFXToolbar();
		header.getStyleClass().add("dialog-header");

		Label titleLabel = new Label(title);
		titleLabel.getStyleClass().add("dialog-title");

		JFXButton closeButton = createIconButton(MaterialDesignIcon.CLOSE, "dialog-close-button");
		closeButton.setOnAction(e -> {
			this.result = false;
			close();
		});

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

		Label messageLabel = new Label(message);
		messageLabel.setWrapText(true);
		messageLabel.getStyleClass().add("message-label");
		VBox messageBox = new VBox(messageLabel);
		messageBox.setAlignment(Pos.CENTER);
		messageBox.setPadding(new Insets(10, 15, 10, 15));

		HBox buttonBox = new HBox(10);
		buttonBox.setAlignment(Pos.CENTER_RIGHT);
		buttonBox.setPadding(new Insets(0, 10, 10, 10));

		JFXButton noButton = new JFXButton("No");
		noButton.getStyleClass().add("no-button");
		noButton.setOnAction(e -> {
			this.result = false;
			close();
		});

		JFXButton yesButton = new JFXButton("Yes");
		yesButton.getStyleClass().add("yes-button");
		yesButton.setOnAction(e -> {
			this.result = true;
			close();
		});

		buttonBox.getChildren().addAll(noButton, yesButton);

		mainContainer.getChildren().addAll(header, messageBox, buttonBox);

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

	public boolean isResult() {
		return result;
	}

	public void setResult(boolean result) {
		this.result = result;
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