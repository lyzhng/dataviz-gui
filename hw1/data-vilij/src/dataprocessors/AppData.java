package dataprocessors;

import algorithms.DataSet;
import algorithms.RandomClassifier;
import javafx.geometry.Point2D;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import settings.AppPropertyTypes;
import ui.AppUI;
import ui.ConfigurationWindow;
import vilij.components.DataComponent;
import vilij.components.ErrorDialog;
import vilij.settings.PropertyTypes;
import vilij.templates.ApplicationTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static settings.AppPropertyTypes.*;
import static vilij.settings.PropertyTypes.*;

/**
 * This is the concrete application-specific implementation of the data component defined by the Vilij framework.
 *
 * @author Ritwik Banerjee
 * @see DataComponent
 */
public class AppData implements DataComponent {

    private TSDProcessor        processor;
    private ApplicationTemplate applicationTemplate;
    RandomClassifier randomClassifier;


    public AppData(ApplicationTemplate applicationTemplate) {
        this.processor = new TSDProcessor();
        this.applicationTemplate = applicationTemplate;
    }

    @Override
    public void loadData(Path dataFilePath) {
        applicationTemplate.getUIComponent().clear();
        clear();
        // int lineCounter = 0;
        // boolean moreThanTen = false;
        try {
            Text statsText = ((AppUI) applicationTemplate.getUIComponent()).getStatsText();
            TextArea textArea = ((AppUI) applicationTemplate.getUIComponent()).getTextArea();
            textArea.clear();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(dataFilePath.toFile()));
            String lineRead;
            String lines = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.EMPTY_STRING.name());
            while ((lineRead = bufferedReader.readLine()) != null) {
                lines += lineRead + System.lineSeparator();
                // lineCounter++;
                /* if (lineCounter == 10)
                    textArea.setText(lines.substring(0, lines.length()-1));
                    */
                /* if (lineCounter > 10)
                    moreThanTen = true; */
            }
            /* if (moreThanTen) {
                ErrorDialog errorDialog = ErrorDialog.getDialog();
                String errDialogTitle = applicationTemplate.manager.getPropertyValue(LOAD_WARNING_TITLE.name());
                String errDialogMsg = String.format(applicationTemplate.manager.getPropertyValue(OVER_TEN_LINES.name()), lineCounter);
                errorDialog.show(errDialogTitle, errDialogMsg);
                setTextAreaActions(Arrays.asList(lines.split(System.lineSeparator())));
            } else {
                textArea.setText(lines);
            } */
            textArea.setText(lines);
            textArea.setEditable(false);
            textArea.getStylesheets().add((getClass().getResource(applicationTemplate.manager.getPropertyValue(TEXTAREA_CSS.name())).toExternalForm()));
            loadData(lines);
            String statsWithPath = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.STATS_WITH_PATH.name());
            statsText.setText(String.format(statsWithPath, processor.getLineNumber().get()-1, getNumberOfLabels(), dataFilePath.toString(), getLabelNames()));

            displayData();
        }
        catch (IOException e) { System.err.println(e.getMessage()); }
    }

    public void loadData(String dataString) {
        try {
            if (dataString.isEmpty()) return;
            processor.processString(dataString);
            int indexErr = checkForDuplicates(dataString);

            if (indexErr != -1) {
                List<String> lines = Arrays.asList(dataString.split(System.lineSeparator()));
                String errLoadTitle = applicationTemplate.manager.getPropertyValue(LOAD_ERROR_TITLE.name());
                String errDupMsg = applicationTemplate.manager.getPropertyValue(DUPLICATE_ERR_MSG.name());
                String errDupCont = (indexErr+1) + System.lineSeparator() + lines.get(indexErr);
                ErrorDialog errorDialog = ErrorDialog.getDialog();
                errorDialog.show(errLoadTitle, errDupMsg + errDupCont);
                ((AppUI) applicationTemplate.getUIComponent()).getSaveButton().setDisable(true);
                processor.hadAnError.set(true);
                clear();
            } else {
                processor.hadAnError.set(false);
            }

        } catch (Exception e) {
            // TODO: check if substring always works
            String errLoadTitle = applicationTemplate.manager.getPropertyValue(PropertyTypes.LOAD_ERROR_TITLE.name());
            String errLoadMsg =
                    applicationTemplate.manager.getPropertyValue(INCORRECT_FORMAT.name()) +
                    e.getMessage().substring(e.getMessage().indexOf("\n")+1);
            ErrorDialog errorDialog = ErrorDialog.getDialog();
            errorDialog.show(errLoadTitle, errLoadMsg);
            ((AppUI) applicationTemplate.getUIComponent()).getSaveButton().setDisable(true);
            processor.hadAnError.set(true);
        }
    }

    @Override
    public void saveData(Path dataFilePath) {
        // NOTE: completing this method was not a part of HW 1. You may have implemented file saving from the
        // confirmation dialog elsewhere in a different way.
        try (PrintWriter writer = new PrintWriter(Files.newOutputStream(dataFilePath))) {
            writer.write(((AppUI) applicationTemplate.getUIComponent()).getCurrentText());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void clear() {
        processor.clear();
    }

    public void displayData() {
        LineChart<Number, Number> chart = ((AppUI) applicationTemplate.getUIComponent()).getChart();
        chart.setAnimated(true);
        processor.toChartData(chart);
        String chartSeriesLine = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_SERIES_LINE.name());
        String avgSeries = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES.name());
        String nullStroke = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.NULL_STROKE.name());
        chart.lookupAll(chartSeriesLine).forEach(node -> {
            if (!(node.lookup(avgSeries) == node))
                node.setStyle(nullStroke);
        });

        /*
       chart.lookupAll(".chart-series-line").forEach(node -> {
            if (!(node.lookup("#avg-series") == node))
                node.setStyle("-fx-stroke:null;");
        });
         */
        ((AppUI) applicationTemplate.getUIComponent()).setTooltips();

    }

    public int checkForDuplicates(String tsdString)
    {
        List<String> names = new ArrayList<>();
        Stream.of(tsdString.split("\n")).map(line -> Arrays.asList(line.split("\t"))).forEach(list -> {
            String name = list.get(0);
            names.add(name);
        });
        List<String> namesFromMap = new ArrayList<>(getDataPoints().keySet());
        if (names.size() != namesFromMap.size()) {
            for (int i = 0; i < namesFromMap.size(); i++) {
                if (!namesFromMap.get(i).equals(names.get(i)))
                    return i;
            }
            return namesFromMap.size();
        }
        return -1;
    }

    /**
     * Helper method for loadData.
     * When there are fewer than 10 lines in oldValue,
     * the newValue should add on to the text area.
     */

    private void setTextAreaActions(List<String> lines) {
        // assuming that there exists > 10 lines, inputted
        AtomicInteger index = new AtomicInteger(10);
        TextArea textArea = ((AppUI) applicationTemplate.getUIComponent()).getTextArea();
        if (textArea.getText().isEmpty()) return;
        textArea.textProperty().addListener(((observable, oldValue, newValue) -> {
            String[] before = oldValue.split(System.lineSeparator());
            String[] after = newValue.split(System.lineSeparator());
            if (before.length == 10 && after.length < 10) {
                String s = "";
                for (int i = 0; i < before.length - after.length; i++) {
                    if (index.get() < lines.size()) {
                        s += lines.get(index.get()) + System.lineSeparator();
                        index.getAndIncrement();
                     } else {
                        break;
                    }
                }
                if (s.length() > 0)
                    textArea.setText(newValue + s.substring(0, s.length()-1));
            }
        }));
    }

    public LinkedHashMap<String, Point2D> getDataPoints() { return processor.getDataPoints(); }

    /* private double findAvgY() {
        LinkedHashMap<String, Point2D> dataPoints = ((AppData) applicationTemplate.getDataComponent()).getDataPoints();
        LineChart<Number, Number> chart = ((AppUI) applicationTemplate.getUIComponent()).getChart();
        double yTotal = 0.0d;
        for (XYChart.Series<Number, Number> series : chart.getData()) {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                yTotal += data.getYValue().doubleValue();
            }
        }
        return yTotal/dataPoints.size();
    } */

    /* private void plotAvgY() {
        LinkedHashMap<String, Point2D> dataPoints = ((AppData) applicationTemplate.getDataComponent()).getDataPoints();
        LineChart<Number, Number> chart = ((AppUI) applicationTemplate.getUIComponent()).getChart();
        List<Double> xValues = new ArrayList<>();
        dataPoints.values().forEach(value -> xValues.add(value.getX()));
        if (xValues.size() <= 1) return;
        if (!xValues.isEmpty()) {
            Double xMin = Collections.min(xValues);
            Double xMax = Collections.max(xValues);
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            Point2D minPoint = new Point2D(xMin, findAvgY());
            Point2D maxPoint = new Point2D(xMax, findAvgY());
            series.getData().add(new XYChart.Data<>(minPoint.getX(), minPoint.getY()));
            series.getData().add(new XYChart.Data<>(maxPoint.getX(), maxPoint.getY()));
            chart.getData().add(series);
            String avgSeriesNorm = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES_NORM.name());
            String chartSeriesLine = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_SERIES_LINE.name());
            String strokeWidth = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES_STROKE_WIDTH.name());
            String chartLineSymbol = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_LINE_SYMBOL.name());
            String bgColor = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES_BG_COLOR.name());
            series.getNode().setId(avgSeriesNorm);
            series.setName(applicationTemplate.manager.getPropertyValue(AVG.name()));

            series.getNode().lookup(chartSeriesLine).setStyle(strokeWidth);

            series.getData().forEach(data ->
                    data.getNode().lookup(chartLineSymbol).setStyle(bgColor));


        }
    } */

    public AtomicBoolean hadAnError() { return processor.hadAnError; }

    public TSDProcessor getProcessor() { return processor; }

    public LinkedHashMap<String, String> getDataLabels() { return processor.getDataLabels(); }

    public int getNumberOfLabels() {
        Set<String> labels = new LinkedHashSet<>(getDataLabels().values());
        labels.removeIf(s -> s.equalsIgnoreCase(applicationTemplate.manager.getPropertyValue(NULL.name())));
        return labels.size();
    }

    public String getLabelNames() {
        Set<String> labels = new LinkedHashSet<>(getDataLabels().values());
        labels.removeIf(s -> s.equalsIgnoreCase(applicationTemplate.manager.getPropertyValue(NULL.name())));
        StringBuilder stringBuilder = new StringBuilder();
        for (String label : labels)
            stringBuilder.append("– ").append(label).append(System.lineSeparator());
        return stringBuilder.toString();
    }

    public void setRunButtonAction() {
        try {
            AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
            ConfigurationWindow configurationWindow = uiComponent.getClassificationWindow();
            DataSet dataset = DataSet.fromTSDProcessor(uiComponent.getCurrentText());
            this.randomClassifier = new RandomClassifier(dataset, applicationTemplate, configurationWindow.getMaxIter(), configurationWindow.getUpdateInterval(), configurationWindow.isContinuousRun());
            new Thread(randomClassifier).start();
        }
        catch (NumberFormatException e) {
            System.out.println("The values have not been set yet.");
        }
    }

    public RandomClassifier getRandomClassifier() { return randomClassifier; }
}
