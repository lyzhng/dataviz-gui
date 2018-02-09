package ui;

import actions.AppActions;
import dataprocessors.AppData;
import dataprocessors.TSDProcessor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import settings.AppPropertyTypes;
import vilij.settings.PropertyTypes;
import vilij.templates.ApplicationTemplate;
import vilij.templates.UITemplate;

import java.nio.file.Paths;

/**
 * This is the application's user interface implementation.
 *
 * @author Ritwik Banerjee
 */
public final class AppUI extends UITemplate {

    /** The application to which this class of actions belongs. */
    ApplicationTemplate applicationTemplate;

    @SuppressWarnings("FieldCanBeLocal")
    private Button                       scrnshotButton; // toolbar button to take a screenshot of the data
    private ScatterChart<Number, Number> chart;          // the chart where data will be displayed
    private Button                       displayButton;  // workspace button to display data on the chart
    private TextArea                     textArea;       // text area for new data input
    private boolean                      hasNewText;     // whether or not the text area has any new data since last display

    public ScatterChart<Number, Number> getChart() { return chart; }

    public AppUI(Stage primaryStage, ApplicationTemplate applicationTemplate) {
        super(primaryStage, applicationTemplate);
        this.applicationTemplate = applicationTemplate;
    }

    @Override
    protected void setResourcePaths(ApplicationTemplate applicationTemplate) {
        super.setResourcePaths(applicationTemplate);
    }

    @Override
    protected void setToolBar(ApplicationTemplate applicationTemplate) {
        // TODO for homework 1
        // Changed
        super.setToolBar(applicationTemplate);
        String iconsPath =
                "/" + String.join("/",
                applicationTemplate.manager.getPropertyValue(PropertyTypes.GUI_RESOURCE_PATH.name()),
                applicationTemplate.manager.getPropertyValue(PropertyTypes.ICONS_RESOURCE_PATH.name()));
        String scrnshotPath =
                String.join("/",
                        iconsPath,
                        applicationTemplate.manager.getPropertyValue(AppPropertyTypes.SCREENSHOT_ICON.name()));
        scrnshotButton =
                setToolbarButton(scrnshotPath,
                        applicationTemplate.manager.getPropertyValue(AppPropertyTypes.SCREENSHOT_TOOLTIP.name()),
                        true);
        toolBar = new ToolBar(newButton, saveButton, loadButton, printButton, exitButton, scrnshotButton);
    }

    @Override
    protected void setToolbarHandlers(ApplicationTemplate applicationTemplate) {
        applicationTemplate.setActionComponent(new AppActions(applicationTemplate));
        newButton.setOnAction(e -> applicationTemplate.getActionComponent().handleNewRequest());
        saveButton.setOnAction(e -> applicationTemplate.getActionComponent().handleSaveRequest());
        loadButton.setOnAction(e -> applicationTemplate.getActionComponent().handleLoadRequest());
        exitButton.setOnAction(e -> applicationTemplate.getActionComponent().handleExitRequest());
        printButton.setOnAction(e -> applicationTemplate.getActionComponent().handlePrintRequest());
    }

    @Override
    public void initialize() {
        layout();
        setWorkspaceActions();
    }

    @Override
    public void clear() {
        // TODO for homework 1
    }

    private void layout() {
        // TODO for homework 1
        BorderPane container = new BorderPane();
        workspace = new Pane();

        VBox for_user = new VBox(5);
        for_user.setMaxWidth(400);
        for_user.setPadding(new Insets(20, 20, 20, 20));
        Label text = new Label("Data File");
        textArea = new TextArea();
        displayButton = new Button("Display");
        for_user.getChildren().addAll(text, textArea, displayButton);
        container.setLeft(for_user);

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        chart = new ScatterChart<>(xAxis, yAxis);
        container.setRight(chart);

        workspace.getChildren().addAll(container);
        appPane.getChildren().addAll(workspace);
        primaryStage.setScene(primaryScene);
    }

    private void setWorkspaceActions() {
        // TODO for homework 1
        ((AppData)(applicationTemplate.getDataComponent())).loadData(textArea.getText());
        // finish loadData
        displayButton.setOnAction(e -> {
            // processString somewhere
            ((AppData)(applicationTemplate.getDataComponent())).displayData();
        });
    }
}
