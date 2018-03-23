package ui;

import actions.AppActions;
import dataprocessors.AppData;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
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
    private Button                       scrnshotButton; // toolbar button to take a screenshot of the data
    private LineChart<Number, Number> chart;          // the chart where data will be displayed
    private Button                       displayButton;  // workspace button to display data on the chart
    private TextArea                       textArea;       // text area for new data input
    private boolean                      hasNewText;     // whether or not the text area has any new data since last display

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

    @Override
    public void initialize() {
        layout();
        setWorkspaceActions();
    }

    @Override
    public void clear() {
        textArea.clear();
        clearChart();
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
        leftPanel.setAlignment(Pos.TOP_CENTER);
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

        HBox processButtonsBox = new HBox();
        displayButton = new Button(manager.getPropertyValue(AppPropertyTypes.DISPLAY_BUTTON_TEXT.name()));
        String read_only = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.READ_ONLY.name());
        CheckBox checkBox = new CheckBox(read_only);
        HBox.setHgrow(processButtonsBox, Priority.ALWAYS);
        processButtonsBox.getChildren().addAll(displayButton, checkBox); // change margin
        checkBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    textArea.setDisable(true);
                } else {
                    textArea.setDisable(false);
                }
            }
        });
        leftPanel.getChildren().addAll(leftPanelTitle, textArea, processButtonsBox);

        StackPane rightPanel = new StackPane(chart);
        rightPanel.setMaxSize(windowWidth * 0.69, windowHeight * 0.69);
        rightPanel.setMinSize(windowWidth * 0.69, windowHeight * 0.69);
        StackPane.setAlignment(rightPanel, Pos.CENTER);

        workspace = new HBox(leftPanel, rightPanel);
        HBox.setHgrow(workspace, Priority.ALWAYS);

        appPane.getChildren().add(workspace);
        VBox.setVgrow(appPane, Priority.ALWAYS);
    }

    private void setWorkspaceActions() {
        setTextAreaActions();
        setDisplayButtonActions();
        toggleScrnshotButton();
    }

    private void toggleScrnshotButton() {
        chart.getData().addListener(new ListChangeListener<XYChart.Series<Number, Number>>() {
            @Override
            public void onChanged(Change<? extends XYChart.Series<Number, Number>> c) {
                if (chart.getData().isEmpty()) {
                    scrnshotButton.setDisable(true);
                } else {
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
                    if (newValue.isEmpty()) {
                        hasNewText = true;
                    } else {
                        hasNewText = false;
                    }
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

    private void setDisplayButtonActions() {
        displayButton.setOnAction(event -> {
//            if (hasNewText) {
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
//            }
        });
    }

    public TextArea getTextArea() {
        return textArea;
    }

    public void clearChart() {
        chart.getData().clear();
    }

    public void setTooltipsActions() {
        LinkedHashMap<String, Point2D> dataPoints = ((AppData) applicationTemplate.getDataComponent()).getDataPoints();
        for (XYChart.Series<Number, Number> series : chart.getData()) {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                
                // Tooltip.install(data.getNode(), new Tooltip(String.format("%s", ...)))
            }
        }
    }
}
