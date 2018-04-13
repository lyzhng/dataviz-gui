package ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import settings.AppPropertyTypes;
import vilij.propertymanager.PropertyManager;
import vilij.templates.ApplicationTemplate;

import java.util.ArrayList;
import java.util.List;

import static settings.AppPropertyTypes.*;

/**
 * @author The author of this document is Lily Zhong.
 */
public class ConfigurationWindow extends Stage
{
    ApplicationTemplate applicationTemplate; // issue here

    private Stage window = new Stage();
    private Button OKButton = new Button();
    private TextField iterField = new TextField();
    private TextField intervalField = new TextField();
    private TextField numClustersField = new TextField();
    private CheckBox checkBox = new CheckBox();
    boolean hasClickedClassification = false;
    boolean hasClickedClustering = false;
    List<Object> classificationPref = new ArrayList<>();
    List<Object> clusteringPref = new ArrayList<>();

    public ConfigurationWindow(ApplicationTemplate applicationTemplate) {
        this.applicationTemplate = applicationTemplate;
    }

    public void init() {
        layout();
        setWorkspaceActions();
    }

    public void layout() {
        PropertyManager manager = applicationTemplate.manager;
        window.setTitle(manager.getPropertyValue(CONFIG_WINDOW_TITLE.name()));

        VBox vBox = new VBox(15);
        HBox hBox = new HBox(10);

        iterField.setPromptText(manager.getPropertyValue(FIELD_PROMPT_TEXT.name()));
        intervalField.setPromptText(manager.getPropertyValue(FIELD_PROMPT_TEXT.name()));

        hBox.getChildren().addAll(new Label(manager.getPropertyValue(MAX_ITERATIONS.name())), iterField);
        hBox.setMaxWidth(300);
        vBox.getChildren().add(hBox);

        hBox = new HBox(10);
        hBox.getChildren().addAll(new Label(manager.getPropertyValue(UPDATE_INTERVAL.name())), intervalField);
        hBox.setMaxWidth(300);
        vBox.getChildren().add(hBox);

        if ((((AppUI) applicationTemplate.getUIComponent()).isSelectedClusteringAlg())) {
            numClustersField.setPromptText(manager.getPropertyValue(FIELD_PROMPT_TEXT.name()));
            hBox = new HBox(10);
            hBox.getChildren().addAll(new Label(manager.getPropertyValue(NUM_CLUSTERS.name())), numClustersField);
            hBox.setMaxWidth(300);
            vBox.getChildren().add(hBox);
        }

        hBox = new HBox(10);
        hBox.getChildren().addAll(new Label(manager.getPropertyValue(CONTINUOUS_RUN.name())), checkBox);
        hBox.setMaxWidth(300);
        vBox.getChildren().add(hBox);

        OKButton.setText(manager.getPropertyValue(APPLY.name()));
        vBox.getChildren().add(OKButton);

        BorderPane pane = new BorderPane();
        vBox.setAlignment(Pos.CENTER);
        pane.setCenter(vBox);

        window.setScene(new Scene(pane, 350, 250));
        window.show();
    }

    private void setWorkspaceActions() {
        setOKButtonActions();
    }

    private void setOKButtonActions() {
        AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
        OKButton.setOnAction(event -> {
            // case for CLASSIFICATION
            boolean validForClassification =
                    iterField.getText().matches("\\d+") &&
                    Integer.parseInt(iterField.getText()) >= 0 &&
                    intervalField.getText().matches("\\d+") &&
                    Integer.parseInt(intervalField.getText()) >= 0;
            // case for CLUSTERING
            boolean validForClustering =
                    validForClassification &&
                    numClustersField.getText().matches("\\d+") &&
                    Integer.parseInt(numClustersField.getText()) >= 0;

            if (validForClassification && uiComponent.isSelectedClassificationAlg() && hasClickedClassification) {
                // load the same settings from classificationPref
                if (!classificationPref.isEmpty()) {
                    iterField.setText((String) classificationPref.get(0));
                    intervalField.setText((String) classificationPref.get(1));
                    checkBox.setSelected((boolean) classificationPref.get(2));
                }
                classificationPref.clear();
                uiComponent.getRunButton().setDisable(false);
            }
            else if (validForClustering && uiComponent.isSelectedClusteringAlg() && hasClickedClustering) {
                // load the same settings from clusteringPref
                if (!clusteringPref.isEmpty()) {
                    iterField.setText((String) clusteringPref.get(0));
                    intervalField.setText((String) clusteringPref.get(1));
                    numClustersField.setText((String) clusteringPref.get(2));
                    checkBox.setSelected((boolean) clusteringPref.get(3));
                }
                clusteringPref.clear();
                uiComponent.getRunButton().setDisable(false);
                // what to do with list?
            }
            else if (validForClassification && uiComponent.isSelectedClassificationAlg()) {
                classificationPref.add(iterField.getText());
                classificationPref.add(intervalField.getText());
                classificationPref.add(checkBox.isSelected());
                hasClickedClassification = true;
                uiComponent.getRunButton().setDisable(false);
            }
            else if (validForClustering && uiComponent.isSelectedClusteringAlg()) {
                clusteringPref.add(iterField.getText());
                clusteringPref.add(intervalField.getText());
                clusteringPref.add(numClustersField.getText());
                clusteringPref.add(checkBox.isSelected());
                hasClickedClustering = true;
                uiComponent.getRunButton().setDisable(false);
            }
            else {
                // invalid input!
                // general cases
                iterField.setText("1");
                intervalField.setText("1");
                checkBox.setSelected(false);
                if (uiComponent.isSelectedClusteringAlg()) {
                    numClustersField.setText("1");
                    hasClickedClustering = true;
                }
                else if (uiComponent.isSelectedClassificationAlg()) {
                    hasClickedClassification = true;
                }
            }
            window.close();
        });
    }

    private void clearClassificationPref() { classificationPref.clear(); }
    private void clearClusteringPref() { clusteringPref.clear(); }
}
