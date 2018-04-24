package ui;

import algorithms.*;
import actions.AppActions;
import dataprocessors.AppData;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import settings.AppPropertyTypes;
import vilij.propertymanager.PropertyManager;
import vilij.templates.ApplicationTemplate;
import vilij.templates.UITemplate;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static settings.AppPropertyTypes.*;
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
    ConfigurationWindow classificationWindow;
    ConfigurationWindow clusteringWindow;

    @SuppressWarnings("FieldCanBeLocal")
    private Button scrnshotButton; // toolbar button to take a screenshot of the data
    private LineChart<Number, Number> chart;          // the chart where data will be displayed
    private TextArea textArea;       // text area for new data input
    private boolean hasNewText;     // whether or not the text area has any new data since last display

    private Text statsText = new Text();
    private ComboBox<String> algorithmSel = new ComboBox<>();
    private Button toggle = new Button();
    private VBox vbox = new VBox();
    private RadioButton classificationAlg = new RadioButton();
    private RadioButton clusteringAlg = new RadioButton();
    private Button runButton = new Button();
    boolean selectedClusteringAlg = false;
    boolean selectedClassificationAlg = false;

    public LineChart<Number, Number> getChart() { return chart; }

    public AppUI(Stage primaryStage, ApplicationTemplate applicationTemplate) {
        super(primaryStage, applicationTemplate);
        this.applicationTemplate = applicationTemplate;
        classificationWindow = new ConfigurationWindow(applicationTemplate);
        clusteringWindow = new ConfigurationWindow(applicationTemplate);
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
                                              manager.getPropertyValue(SCREENSHOT_ICON.name()));
        scrnshotButton = setToolbarButton(scrnshoticonPath,
                                          manager.getPropertyValue(SCREENSHOT_TOOLTIP.name()),
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
        PropertyManager manager = applicationTemplate.manager;
        AppData dataComponent = ((AppData) applicationTemplate.getDataComponent());
        toggle.setOnAction(e -> {
            if (textArea.getText().isEmpty()) {
                return;
            }
            else if (toggle.getText().equals(manager.getPropertyValue(DONE.name())) && !textArea.getText().isEmpty()) {
                clearChart();
                applicationTemplate.getDataComponent().clear();
                // loading, updating, displaying
                dataComponent.loadData(textArea.getText());
                String statsWithoutPath = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.STATS_WITHOUT_PATH.name());
                statsText.setText(String.format(statsWithoutPath, dataComponent.getProcessor().getLineNumber().get()-1, dataComponent.getNumberOfLabels(), dataComponent.getLabelNames()));
                dataComponent.displayData();
                // show or hide statistics and algorithm types
                if (!dataComponent.hadAnError().get())
                    showStatsAndAlgorithm();
                else {
                    hideStats();
                    hideAlgorithmTypes();
                }
                showAlgorithmTypes();
                chosenListHandler();
                radioButtonHandler();

                textArea.setDisable(true);
                toggle.setText(manager.getPropertyValue(EDIT.name()));
            }

            else if (toggle.getText().equals(manager.getPropertyValue(EDIT.name()))) {
                textArea.setDisable(false);
                toggle.setText(manager.getPropertyValue(DONE.name()));
                algorithmSel.getSelectionModel().clearSelection();
                vbox.getChildren().get(0).setManaged(false);
                vbox.getChildren().get(0).setVisible(false);
                vbox.getChildren().get(1).setManaged(false);
                vbox.getChildren().get(1).setVisible(false);
                hideRunButton();
                classificationAlg.setSelected(false);
                clusteringAlg.setSelected(false);
                selectedClusteringAlg = false;
                selectedClassificationAlg = false;
                hideAlgorithmTypes();
            }
        });
    }

    public void hideAlgorithmMethods() {
        vbox.getChildren().get(0).setManaged(false);
        vbox.getChildren().get(0).setVisible(false);
    }

    @Override
    public void initialize() {
        layout();
        setWorkspaceActions();
    }

    @Override
    public void clear() {
        textArea.getStylesheets().clear();
        textArea.clear();
        clearChart();
        resetToggleText();
        enableTextArea();
        hideTextArea();
        hideStats();
        hideAlgorithmTypes();
        hideToggles();
        hideAlgorithmLists();
        if (algorithmSel.getSelectionModel().getSelectedItem() != null)
            algorithmSel.getSelectionModel().clearSelection();
        hideRunButton();
        classificationAlg.setSelected(false);
        clusteringAlg.setSelected(false);
        selectedClusteringAlg = false;
        selectedClassificationAlg = false;
    }

    public void hideRunButton() {
        runButton.setVisible(false);
        runButton.setManaged(false);
    }

    public void showRunButton() {
        runButton.setVisible(true);
        runButton.setManaged(true);
    }

    private void layout() {
        PropertyManager manager = applicationTemplate.manager;
        NumberAxis      xAxis   = new NumberAxis();
        NumberAxis      yAxis   = new NumberAxis();
        chart = new LineChart<>(xAxis, yAxis);

        chart.getStylesheets().add(getClass().getResource(manager.getPropertyValue(CHART_CSS.name())).toExternalForm());
        // external spreadsheet to chart_style.css

        chart.setTitle(manager.getPropertyValue(CHART_TITLE.name()));
        chart.setHorizontalGridLinesVisible(false);
        chart.setVerticalGridLinesVisible(false);
        chart.setHorizontalZeroLineVisible(false);
        chart.setVerticalZeroLineVisible(false);

        VBox leftPanel = new VBox(8);
        leftPanel.setAlignment(Pos.TOP_LEFT);
        leftPanel.setPadding(new Insets(10));

        VBox.setVgrow(leftPanel, Priority.ALWAYS);
        leftPanel.setMaxSize(windowWidth * 0.29, windowHeight * 0.3);
        leftPanel.setMinSize(windowWidth * 0.29, windowHeight * 0.3);

        Text   leftPanelTitle = new Text(manager.getPropertyValue(LEFT_PANE_TITLE.name()));
        String fontname       = manager.getPropertyValue(LEFT_PANE_TITLEFONT.name());
        Double fontsize       = Double.parseDouble(manager.getPropertyValue(LEFT_PANE_TITLESIZE.name()));
        leftPanelTitle.setFont(Font.font(fontname, fontsize));

        textArea = new TextArea();
        textArea.setMinHeight(200);
        leftPanelTitle.setVisible(false);
        leftPanelTitle.setManaged(false);
        hideTextArea();
        statsText.setWrappingWidth(windowWidth * 0.29);
        algorithmSelInit();
        hideStats();
        hideAlgorithmTypes();
        resetToggleText();
        hideToggles();
        algorithmListInit();

        leftPanel.getChildren().addAll(leftPanelTitle, textArea, toggle, statsText, algorithmSel);

        HBox hbox = new HBox();
        hbox.getChildren().addAll(classificationAlg, new Button(manager.getPropertyValue(CONFIGURATION.name())));
        vbox.getChildren().add(hbox);
        hbox = new HBox();
        hbox.getChildren().addAll(clusteringAlg, new Button(manager.getPropertyValue(CONFIGURATION.name())));
        vbox.getChildren().add(hbox);
        leftPanel.getChildren().add(vbox);

        runButton.setText(manager.getPropertyValue(RUN.name()));
        runButton.setTooltip(new Tooltip(manager.getPropertyValue(RUN.name())));
        leftPanel.getChildren().add(runButton);
        runButton.setVisible(false);
        runButton.setManaged(false);

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

    private void algorithmSelInit() {
        algorithmSel.getItems().add(applicationTemplate.manager.getPropertyValue(CLASSIFICATION.name()));
        algorithmSel.getItems().add(applicationTemplate.manager.getPropertyValue(CLUSTERING.name()));
        algorithmSel.setPromptText(applicationTemplate.manager.getPropertyValue(ALGORITHM_TYPE.name()));
    }

    private void setWorkspaceActions() {
        setTextAreaActions();
        toggleScrnshotButton();
        setToggleHandler();
        configButtonHandler();
        setRunHandler();
    }

    public void setRunHandler() {
        AppData dataComponent = ((AppData) applicationTemplate.getDataComponent());
        runButton.setOnAction(event -> dataComponent.setRunButtonAction());
    }

    private void algorithmListInit() {
        ToggleGroup group = new ToggleGroup();
        classificationAlg.setText(applicationTemplate.manager.getPropertyValue(RANDOM_CLASSIFICATION.name()));
        clusteringAlg.setText(applicationTemplate.manager.getPropertyValue(RANDOM_CLUSTERING.name()));
        classificationAlg.setToggleGroup(group);
        clusteringAlg.setToggleGroup(group);
        hideAlgorithmLists();
    }

    public void hideAlgorithmLists() {
        vbox.setVisible(false);
        vbox.setManaged(false);
    }

    private void toggleScrnshotButton() {
        chart.getData().addListener((ListChangeListener<XYChart.Series<Number, Number>>) c -> {
            RandomClassifier randomClassifier = ((AppData) applicationTemplate.getDataComponent()).getRandomClassifier();
            if (chart.getData().isEmpty()) {
                scrnshotButton.setDisable(true);
            }
            else {
                scrnshotButton.setDisable(false);
            }
            if (randomClassifier != null) {
                if (randomClassifier.isRunningProperty().get()) {
                    scrnshotButton.setDisable(true);
                }
                else {
                    scrnshotButton.setDisable(false);
                }
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
    private void resetToggleText() { toggle.setText(applicationTemplate.manager.getPropertyValue(DONE.name())); }
    public String getCurrentText() { return textArea.getText(); }

    public void hideStats() {
        statsText.setVisible(false);
        statsText.setManaged(false);
    }

    public void hideAlgorithmTypes() {
        algorithmSel.setVisible(false);
        algorithmSel.setManaged(false);
    }

    public void showTextArea() {
        textArea.setVisible(true);
        textArea.setManaged(true);
    }

    public void hideTextArea() {
        textArea.setManaged(false);
        textArea.setVisible(false);
    }

    public void showStatsAndAlgorithm() {
        statsText.setVisible(true);
        statsText.setManaged(true);
        algorithmSel.setVisible(true);
        algorithmSel.setManaged(true);
    }

    public void showToggles() {
        toggle.setVisible(true);
        toggle.setManaged(true);
    }

    public void enableTextArea() {
        textArea.setDisable(false);
    }

    public void disableTextArea() {
        textArea.setDisable(true);
    }

    public void hideToggles() {
       toggle.setVisible(false);
       toggle.setManaged(false);
    }

    public void showAlgorithmTypes() {
        AppData dataComponent = (AppData) applicationTemplate.getDataComponent();
        if (dataComponent.getNumberOfLabels() != 2 && getAlgorithmSel().getItems().size() == 2)
            getAlgorithmSel().getItems().remove(0);
        if (dataComponent.getNumberOfLabels() == 2 && getAlgorithmSel().getItems().size() != 2) // only showing clustering
            getAlgorithmSel().getItems().add(0, applicationTemplate.manager.getPropertyValue(CLASSIFICATION.name()));
    }

    // combo box as a whole
    public void chosenListHandler() {

        /* if there is a selected item, it will hide the ComboBox and show the respective algorithm's name */
        algorithmSel.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (algorithmSel.getSelectionModel().getSelectedItem() != null) {
                vbox.setVisible(true);
                vbox.setManaged(true);
                if (getAlgorithmSel().getSelectionModel().getSelectedItem().equalsIgnoreCase(applicationTemplate.manager.getPropertyValue(CLASSIFICATION.name()))) {
                    vbox.getChildren().get(0).setVisible(true);
                    vbox.getChildren().get(0).setManaged(true);
                    vbox.getChildren().get(1).setVisible(false);
                    vbox.getChildren().get(1).setManaged(false);
                    selectedClassificationAlg = true;
                }
                if (algorithmSel.getSelectionModel().getSelectedItem().equalsIgnoreCase(applicationTemplate.manager.getPropertyValue(CLUSTERING.name()))) {
                    vbox.getChildren().get(0).setVisible(false);
                    vbox.getChildren().get(0).setManaged(false);
                    vbox.getChildren().get(1).setVisible(true);
                    vbox.getChildren().get(1).setManaged(true);
                    selectedClusteringAlg = true;
                }
            }
            hideAlgorithmTypes();
        });
    }

    public void radioButtonHandler() {
        classificationAlg.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (classificationAlg.isSelected()) {
                showRunButton();
                runButton.setDisable(true);
            }
        });
        clusteringAlg.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (clusteringAlg.isSelected()) {
                showRunButton();
                runButton.setDisable(true);
            }
        });
    }

    public void configButtonHandler() {
        ((Button) ((HBox) vbox.getChildren().get(0)).getChildren().get(1)).setOnAction(event -> {
            classificationWindow.init();
        });
        ((Button) ((HBox) vbox.getChildren().get(1)).getChildren().get(1)).setOnAction(event -> {
            clusteringWindow.init();
        });
    }

    public boolean isSelectedClusteringAlg() { return selectedClusteringAlg; }

    public boolean isSelectedClassificationAlg() { return selectedClassificationAlg; }

    public Button getRunButton() { return runButton; }

    public ConfigurationWindow getClassificationWindow() { return classificationWindow; }

    public ConfigurationWindow getClusteringWindow() { return clusteringWindow; }

    public Button getScrnshotButton() { return scrnshotButton; }
}
