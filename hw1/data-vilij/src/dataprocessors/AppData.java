package dataprocessors;

import javafx.geometry.Point2D;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextArea;
import ui.AppUI;
import vilij.components.DataComponent;
import vilij.components.ErrorDialog;
import vilij.settings.PropertyTypes;
import vilij.templates.ApplicationTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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


    public AppData(ApplicationTemplate applicationTemplate) {
        this.processor = new TSDProcessor();
        this.applicationTemplate = applicationTemplate;
    }

    @Override
    public void loadData(Path dataFilePath) {
        int lineCounter = 0;
        boolean moreThanTen = false;
        // last iteration of while loop, do not end with line separator
        try {
            TextArea textArea = ((AppUI) applicationTemplate.getUIComponent()).getTextArea();
            textArea.clear();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(dataFilePath.toFile()));
            String lineRead;
            String lines = "";
            while ((lineRead = bufferedReader.readLine()) != null) {
                lines += lineRead + System.lineSeparator();
                lineCounter++;
                if (lineCounter == 10)
                    textArea.setText(lines.substring(0, lines.length()-1));
                if (lineCounter > 10)
                    moreThanTen = true;
            }
            if (moreThanTen) {
                ErrorDialog errorDialog = ErrorDialog.getDialog();
                String errDialogTitle = applicationTemplate.manager.getPropertyValue(LOAD_WARNING_TITLE.name());
                String errDialogMsg = String.format(applicationTemplate.manager.getPropertyValue(OVER_TEN_LINES.name()), lineCounter);
                errorDialog.show(errDialogTitle, errDialogMsg);
                setTextAreaActions(Arrays.asList(lines.split(System.lineSeparator())));
            } else {
                textArea.setText(lines);
            }
            loadData(lines);
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
                String errDupCont = (indexErr + 1) + System.lineSeparator() + lines.get(indexErr);
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
                    applicationTemplate.manager.getPropertyValue(OCCURRED_AT.name()) +
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
        processor.toChartData(chart);
        plotAvgY();
        //  make lines invisible here
        chart.lookupAll(".chart-series-line").forEach(node -> {
            if (!(node.lookup("#avg-series") == node))
                node.setStyle("-fx-stroke:null;");
        });
        ((AppUI) applicationTemplate.getUIComponent()).setTooltips();
    }

    private int checkForDuplicates(String tsdString)
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
                // before - after <- how many lines to show
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

    public LinkedHashMap<String, Point2D> getDataPoints() {
        return processor.getDataPoints();
    }

    private double findAvgY() {
        LinkedHashMap<String, Point2D> dataPoints = ((AppData) applicationTemplate.getDataComponent()).getDataPoints();
        LineChart<Number, Number> chart = ((AppUI) applicationTemplate.getUIComponent()).getChart();
        double yTotal = 0.0d;
        for (XYChart.Series<Number, Number> series : chart.getData()) {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                yTotal += data.getYValue().doubleValue();
            }
        }
        return yTotal/dataPoints.size();
    }

    private void plotAvgY() {
        LinkedHashMap<String, Point2D> dataPoints = ((AppData) applicationTemplate.getDataComponent()).getDataPoints();
        LineChart<Number, Number> chart = ((AppUI) applicationTemplate.getUIComponent()).getChart();
        List<Double> xValues = new ArrayList<>();
        dataPoints.values().forEach(value -> xValues.add(value.getX()));
        if (xValues.size() <= 1) return;
        Collections.sort(xValues);
        if (!xValues.isEmpty()) {
            Double xMin = xValues.get(0);
            Double xMax = xValues.get(xValues.size() - 1);
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            Point2D minPoint = new Point2D(xMin, findAvgY());
            Point2D maxPoint = new Point2D(xMax, findAvgY());
            series.getData().add(new XYChart.Data<>(minPoint.getX(), minPoint.getY()));
            series.getData().add(new XYChart.Data<>(maxPoint.getX(), maxPoint.getY()));
            chart.getData().add(series);
            series.getNode().setId("avg-series");
            series.setName("AVG");
            series.getNode().lookup(".chart-series-line").setStyle(
                    "-fx-stroke: #0099ff; -fx-stroke-width: 4px;"
            );
            series.getData().forEach(data ->
                    data.getNode().lookup(".chart-line-symbol").setStyle("-fx-background-color: transparent;"));
        }
    }

    public AtomicBoolean hadAnError() {
        return processor.hadAnError;
    }
}
