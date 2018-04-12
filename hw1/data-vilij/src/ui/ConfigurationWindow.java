package ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import vilij.templates.ApplicationTemplate;

/**
 * @author The author of this document is Lily Zhong.
 */
public class ConfigurationWindow extends Stage
{
    ApplicationTemplate applicationTemplate;
    Stage window = new Stage();
    Button OKButton = new Button();
    TextField iterField = new TextField();
    TextField intervalField = new TextField();

    public void init() {
        layout();
        setWorkspaceActions();
    }

    public void layout() {
        window.setTitle("Algorithm Run Configuration");

        VBox vBox = new VBox(15);
        HBox hBox = new HBox(10);
        TextField iterField = new TextField();
        TextField intervalField = new TextField();
        CheckBox checkBox = new CheckBox();

        hBox.getChildren().addAll(new Label("Max Iterations"), iterField);
        hBox.setMaxWidth(300);
        vBox.getChildren().add(hBox);

        hBox = new HBox(10);
        hBox.getChildren().addAll(new Label("Update Interval"), intervalField);
        hBox.setMaxWidth(300);
        vBox.getChildren().add(hBox);

        if ((((AppUI) applicationTemplate.getUIComponent()).isSelectedClusteringAlg())) {
            hBox = new HBox(10);
            hBox.getChildren().addAll(new Label("Number of Labels"));
            hBox.setMaxWidth(300);
            vBox.getChildren().add(hBox);
        }

        hBox = new HBox(10);
        hBox.getChildren().addAll(new Label("Continuous Run?"), checkBox);
        hBox.setMaxWidth(300);
        vBox.getChildren().add(hBox);

        Button okButton = new Button("OK");
        vBox.getChildren().add(okButton);

        BorderPane pane = new BorderPane();
        vBox.setAlignment(Pos.CENTER);
        pane.setCenter(vBox);

        window.setScene(new Scene(pane, 350, 200));
        window.show();
    }

    public void setWorkspaceActions() {
        setOKButtonActions();
    }

    public void setOKButtonActions() {
        OKButton.setOnAction(event -> {
            boolean valid = iterField.getText().matches("\\d+") &&
                    Integer.parseInt(iterField.getText()) >= 0 &&
                    intervalField.getText().matches("\\d+") &&
                    Integer.parseInt(intervalField.getText()) >= 0;
            if (valid) {
                // save the data
                // set boolean to true (enable run button)
                // have exterior listener for run button
            } else {
                // set text fields to default 0
            }
            window.close();
        });
    }
}
