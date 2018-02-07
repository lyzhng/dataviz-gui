package ui;

import actions.AppActions;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.ScatterChart;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
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
        /* scrnshotButton = setToolbarButton("/Users/lilyzhong/IdeaProjects/cse219homework/hw1/data-vilij/resources/gui/icons/screenshot.png",
                "Screenshot",
                false); */
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
        // Left side of the layout contains what the user wants to do.
        VBox userRegulated = new VBox();
        Label text = new Label("Data File");
        textArea = new TextArea();
        displayButton = new Button("Display");
        userRegulated.getChildren().addAll(text, textArea, displayButton);

        // Why doesn't userRegulated.setPrefWidth(400); work?

        userRegulated.setMaxWidth(400);
        userRegulated.setPadding(new Insets(50, 50, 50, 50));
        workspace = new Pane();
        workspace.getChildren().add(userRegulated);
        appPane.getChildren().add(workspace);
        primaryStage.setScene(primaryScene);

        // Right side of the layout is what is generated based on input.



    }

    private void setWorkspaceActions() {
        // TODO for homework 1
        // Display button
    }
}
