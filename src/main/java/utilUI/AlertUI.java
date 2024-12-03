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
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AlertUI extends Stage {
    private double xOffset = 0;
    private double yOffset = 0;

    public AlertUI(Stage owner, String title, String message) {
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

        Text messageText = new Text(message);
		messageText.setTextAlignment(TextAlignment.CENTER);
		TextFlow messageFlow = new TextFlow(messageText);
		messageFlow.setTextAlignment(TextAlignment.CENTER);
		messageFlow.setPrefWidth(260);
		messageFlow.setLineSpacing(1); // Tùy chỉnh khoảng cách giữa các dòng

		VBox messageBox = new VBox(messageFlow);
		messageBox.setAlignment(Pos.CENTER);
		messageBox.setPadding(new Insets(10, 15, 10, 15));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(0, 10, 10, 10));

        JFXButton okButton = new JFXButton("OK");
        okButton.getStyleClass().add("ok-button");
        okButton.setOnAction(e -> close());

        buttonBox.getChildren().add(okButton);

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

    private JFXButton createIconButton(MaterialDesignIcon icon, String styleClass) {
        JFXButton button = new JFXButton();
        MaterialDesignIconView iconView = new MaterialDesignIconView(icon);
        iconView.setSize("16");
        button.setGraphic(iconView);
        button.getStyleClass().add(styleClass);
        return button;
    }

    public static void show(Stage owner, String title, String message) {
        AlertUI alert = new AlertUI(owner, title, message);
        alert.showAndWait();
    }
}