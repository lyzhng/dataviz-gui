package ui;

import actions.AppActions;
import dataprocessors.AppData;
import dataprocessors.TSDProcessor;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import settings.AppPropertyTypes;
import vilij.propertymanager.PropertyManager;
import vilij.templates.ApplicationTemplate;
import vilij.templates.UITemplate;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.io.File.separator;
import static vilij.settings.PropertyTypes.GUI_RESOURCE_PATH;
import static vilij.settings.PropertyTypes.ICONS_RESOURCE_PATH;

/**
 * This is the application's user interface implementation.
 *
 * @author Ritwik Banerjee
 */
public final class AppUI extends UITemplate {

    /** The application to which this class of actions belongs. */
    ApplicationTemplate applicationTemplate;

    @SuppressWarnings("FieldCanBeLocal")
    private Button scrnshotButton; // toolbar button to take a screenshot of the data
    private LineChart<Number, Number> chart;          // the chart where data will be displayed
    // private Button displayButton;  // workspace button to display data on the chart
    private TextArea textArea;       // text area for new data input
    private boolean hasNewText;     // whether or not the text area has any new data since last display
    private Text statsText = new Text();
    private ComboBox<String> algorithmSel = new ComboBox<>();
    private ToggleButton toggleButton;

    public LineChart<Number, Number> getChart() { return chart; }

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
        super.setToolBar(applicationTemplate);
        PropertyManager manager = applicationTemplate.manager;
        String iconsPath = SEPARATOR + String.join(SEPARATOR,
                                                   manager.getPropertyValue(GUI_RESOURCE_PATH.name()),
                                                   manager.getPropertyValue(ICONS_RESOURCE_PATH.name()));
        String scrnshoticonPath = String.join(SEPARATOR,
                                              iconsPath,
                                              manager.getPropertyValue(AppPropertyTypes.SCREENSHOT_ICON.name()));
        scrnshotButton = setToolbarButton(scrnshoticonPath,
                                          manager.getPropertyValue(AppPropertyTypes.SCREENSHOT_TOOLTIP.name()),
                                          true);
        toolBar.getItems().add(scrnshotButton);
    }

    @Override
    protected void setToolbarHandlers(ApplicationTemplate applicationTemplate) {
        applicationTemplate.setActionComponent(new AppActions(applicationTemplate));
        newButton.setOnAction(e -> applicationTemplate.getActionComponent().handleNewRequest());
        saveButton.setOnAction(e -> applicationTemplate.getActionComponent().handleSaveRequest());
        loadButton.setOnAction(e -> applicationTemplate.getActionComponent().handleLoadRequest());
        exitButton.setOnAction(e -> applicationTemplate.getActionComponent().handleExitRequest());
        printButton.setOnAction(e -> applicationTemplate.getActionComponent().handlePrintRequest());
        scrnshotButton.setOnAction(e -> ((AppActions) (applicationTemplate.getActionComponent())).handleScreenshotRequest());
    }

    protected void setToggleHandler() {
        AppData dataComponent = ((AppData) applicationTemplate.getDataComponent());
        AppActions actionComponent = ((AppActions) applicationTemplate.getActionComponent());
        toggleButton.setOnAction(e -> {
            if (textArea.getText().isEmpty()) toggleButton.setSelected(false);
            else if (toggleButton.getText().equals("Done") && !textArea.getText().isEmpty()) {
                clearChart();
                applicationTemplate.getDataComponent().clear();

                dataComponent.loadData(textArea.getText());
                statsText.setText(String.format("%d instance(s) with %d label(s). The label(s) are: \n%s", dataComponent.getProcessor().getLineNumber().get()-1, dataComponent.getNumberOfLabels(), dataComponent.getLabelNames()));
                dataComponent.displayData();

                if (!dataComponent.hadAnError().get()) actionComponent.showStatsAndAlgorithm();
                else actionComponent.hideStatsAndAlgorithm();

                textArea.setDisable(true);
                toggleButton.setText("Edit");
                toggleButton.setSelected(false);
            }
            else if (toggleButton.getText().equals("Edit")) {
                textArea.setDisable(false);
                toggleButton.setText("Done");
                toggleButton.setSelected(false);
            }
        });
    }

    @Override
    public void initialize() {
        layout();
        setWorkspaceActions();
    }

    @Override
    public void clear() {
        AppActions appActions = ((AppActions) applicationTemplate.getActionComponent());
        textArea.clear();
        clearChart();
        toggleButton.setText("Done");
        appActions.hideTextArea();
        appActions.hideStatsAndAlgorithm();
        appActions.hideToggles();
    }

    public String getCurrentText() { return textArea.getText(); }

    private void layout() {
        PropertyManager manager = applicationTemplate.manager;
        NumberAxis      xAxis   = new NumberAxis();
        NumberAxis      yAxis   = new NumberAxis();
        chart = new LineChart<>(xAxis, yAxis);

        // external spreadsheet to chart_style.css
        String dirPath = separator + applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CSS_RESOURCE_PATH.name());
        URL dirPathURL = getClass().getResource(dirPath);
        String cssPath = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CSS_FILE_NAME.name());
        chart.getStylesheets().add(dirPathURL + separator + cssPath);

        chart.setTitle(manager.getPropertyValue(AppPropertyTypes.CHART_TITLE.name()));
        chart.setHorizontalGridLinesVisible(false);
        chart.setVerticalGridLinesVisible(false);

        VBox leftPanel = new VBox(8);
        leftPanel.setAlignment(Pos.TOP_LEFT);
        leftPanel.setPadding(new Insets(10));

        VBox.setVgrow(leftPanel, Priority.ALWAYS);
        leftPanel.setMaxSize(windowWidth * 0.29, windowHeight * 0.3);
        leftPanel.setMinSize(windowWidth * 0.29, windowHeight * 0.3);

        Text   leftPanelTitle = new Text(manager.getPropertyValue(AppPropertyTypes.LEFT_PANE_TITLE.name()));
        String fontname       = manager.getPropertyValue(AppPropertyTypes.LEFT_PANE_TITLEFONT.name());
        Double fontsize       = Double.parseDouble(manager.getPropertyValue(AppPropertyTypes.LEFT_PANE_TITLESIZE.name()));
        leftPanelTitle.setFont(Font.font(fontname, fontsize));

        textArea = new TextArea();
        textArea.setMinHeight(200);

        // HBox processButtonsBox = new HBox();
        /* displayButton = new Button(manager.getPropertyValue(AppPropertyTypes.DISPLAY_BUTTON_TEXT.name()));
        String read_only = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.READ_ONLY.name());
        CheckBox checkBox = new CheckBox(read_only); */
        // HBox.setHgrow(processButtonsBox, Priority.ALWAYS);
        // processButtonsBox.getChildren().addAll(displayButton, checkBox); // change margin

        // FIXME: Just added!
        leftPanelTitle.setVisible(false);
        textArea.setVisible(false);
        /* displayButton.setVisible(false);
        checkBox.setVisible(false);

        checkBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    textArea.setDisable(true);
                } else {
                    textArea.setDisable(false);
                }
            }
        }); */

        statsText.setWrappingWidth(windowWidth * 0.29);
        statsText.setVisible(false);
        statsText.setManaged(false);
        // FIXME: Generic
        algorithmSel.getItems().addAll("Classification", "Clustering");
        algorithmSel.setPromptText("Algorithm Type");
        algorithmSel.setVisible(false);
        algorithmSel.setVisible(false);

        toggleButton = new ToggleButton("Done");
        toggleButton.setManaged(false);

        /* leftPanel.getChildren()
                .addAll(leftPanelTitle, textArea, processButtonsBox, statsText, algorithmSel); */
        leftPanel.getChildren()
                .addAll(leftPanelTitle, textArea, toggleButton, statsText, algorithmSel);

        StackPane rightPanel = new StackPane(chart);
        rightPanel.setMaxSize(windowWidth * 0.69, windowHeight * 0.69);
        rightPanel.setMinSize(windowWidth * 0.69, windowHeight * 0.69);
        StackPane.setAlignment(rightPanel, Pos.CENTER);

        workspace = new HBox(leftPanel, rightPanel);
        HBox.setHgrow(workspace, Priority.ALWAYS);

        appPane.getChildren().add(workspace);
        VBox.setVgrow(appPane, Priority.ALWAYS);

        newButton.setDisable(false);
    }

    private void setWorkspaceActions() {
        setTextAreaActions();
        // setDisplayButtonActions();
        toggleScrnshotButton();
        setToggleHandler();
    }

    private void toggleScrnshotButton() {
        chart.getData().addListener((ListChangeListener<XYChart.Series<Number, Number>>) c -> {
            if (chart.getData().isEmpty()) {
                scrnshotButton.setDisable(true);
            } else {
                scrnshotButton.setDisable(false);
            }
        });
    }

    private void setTextAreaActions() {
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!newValue.equals(oldValue)) { // if text is changed
                    ((AppActions) applicationTemplate.getActionComponent()).setIsUnsavedProperty(true);
                    hasNewText = newValue.isEmpty();
                    if (hasNewText) {
                        newButton.setDisable(true);
                        saveButton.setDisable(true);
                    } else {
                        newButton.setDisable(false);
                        saveButton.setDisable(false);
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.println(newValue);
            }
        });
    }

    /* private void setDisplayButtonActions() {
        displayButton.setOnAction(event -> {
                try {
                    AppData dataComponent = (AppData) applicationTemplate.getDataComponent();
                    clearChart();
                    dataComponent.clear();
                    dataComponent.loadData(textArea.getText());
                    dataComponent.displayData();
                    toggleScrnshotButton();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        });
    } */

    /* public Button getDisplayButton() {
        return displayButton;
    } */

    public void setTooltips() {
        LinkedHashMap<String, Point2D> dataPoints = ((AppData) applicationTemplate.getDataComponent()).getDataPoints();
        for (XYChart.Series<Number, Number> series : chart.getData()) {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                Double xValue = data.getXValue().doubleValue();
                Double yValue = data.getYValue().doubleValue();
                Point2D point = new Point2D(xValue, yValue);
                data.getNode().setOnMouseEntered(event -> data.getNode().setCursor(Cursor.CROSSHAIR));
                dataPoints.keySet().forEach(key -> {
                    if (dataPoints.get(key).equals(point)) {
                        Tooltip.install(data.getNode(), new Tooltip(key));
                    }
                });
            }
        }
    }

    public Text getStatsText() { return statsText; }

    public ComboBox<String> getAlgorithmSel() { return algorithmSel; }

    public Button getSaveButton() { return saveButton; }

    public TextArea getTextArea() { return textArea; }

    public void clearChart() { chart.getData().clear(); }

    public ToggleButton getToggleButton() { return toggleButton; }
}
