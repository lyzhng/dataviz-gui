package algorithms;

import dataprocessors.AppData;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.HBox;
import settings.AppPropertyTypes;
import ui.AppUI;
import vilij.settings.PropertyTypes;
import vilij.templates.ApplicationTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static settings.AppPropertyTypes.*;

/**
 * @author Ritwik Banerjee
 */
public class RandomClassifier extends Classifier {

    private static final Random RAND = new Random();

    @SuppressWarnings("FieldCanBeLocal")
    // this mock classifier doesn't actually use the data, but a real classifier will
    private DataSet dataset;
    ApplicationTemplate applicationTemplate;
    private final int maxIterations;
    private final int updateInterval;
    private static int currentIteration = 0;
    private static AtomicReference<XYChart.Series<Number, Number>> prevSeriesRef = new AtomicReference<>();
    ReentrantLock lock = new ReentrantLock();
    private SimpleBooleanProperty finishedRunning = new SimpleBooleanProperty(false); // later

    // currently, this value does not change after instantiation
    private final AtomicBoolean tocontinue;

    @Override
    public int getMaxIterations() {
        return maxIterations;
    }

    @Override
    public int getUpdateInterval() {
        return updateInterval;
    }

    @Override
    public boolean tocontinue() {
        return tocontinue.get();
    }

    public RandomClassifier(DataSet dataset,
                            ApplicationTemplate applicationTemplate,
                            int maxIterations,
                            int updateInterval,
                            boolean tocontinue) {
        this.dataset = dataset;
        this.applicationTemplate = applicationTemplate;
        this.maxIterations = maxIterations;
        this.updateInterval = updateInterval;
        this.tocontinue = new AtomicBoolean(tocontinue);
    }

    @Override
    public void run() {
        // case for continuous run
        Platform.setImplicitExit(true);
        if (tocontinue())
            continuousrun();
        else
            manualrun();
    }

    private void continuousrun() {
        try {
            AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
            AppData dataComponent = ((AppData) applicationTemplate.getDataComponent());
            List<Double> xvalues = new ArrayList<>();
            dataComponent.getDataPoints().values().forEach(value -> xvalues.add(value.getX()));
            double xmin = Collections.min(xvalues);
            double xmax = Collections.max(xvalues);
            finishedRunning.set(false);
            for (int i = 1; i <= maxIterations && tocontinue(); i += 1) {
                Platform.runLater(() -> {
                    uiComponent.getRunButton().setDisable(true);
                    uiComponent.getToggle().setDisable(true);
                    uiComponent.getScrnshotButton().setDisable(true);
                });
                double yForXmin = getYValue(xmin);
                double yForXmax = getYValue(xmax);
                // everything below is just for internal viewing of how the output is changing
                // in the final project, such changes will be dynamically visible in the UI
                if (i % updateInterval == 0) {
                    System.out.printf("Iteration number %d: ", i);
                    flush();
                    XYChart.Series<Number, Number> series = new XYChart.Series<>();
                    series.setName("Random Classifier Line");
                    series.getData().add(new XYChart.Data<>(xmin, yForXmin));
                    series.getData().add(new XYChart.Data<>(xmax, yForXmax));
                    String chartSeriesLine = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_SERIES_LINE.name());
                    String strokeWidth = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES_STROKE_WIDTH.name());
                    String chartLineSymbol = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_LINE_SYMBOL.name());
                    String bgColor = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES_BG_COLOR.name());
                    Platform.runLater(() -> {
                        lock.lock();
                        try {
                            uiComponent.getChart().getData().add(series);
                            series.getNode().lookup(chartSeriesLine).setStyle(strokeWidth);
                            series.getData().forEach(data -> data.getNode().lookup(chartLineSymbol).setStyle(bgColor));
                        } finally {
                            lock.unlock();
                        }
                    });
                    Thread.sleep(500);
                    if (i + updateInterval <= maxIterations) {
                        Platform.runLater(() -> {
                            lock.lock();
                            try {
                                uiComponent.getChart().getData().remove(series);
                            } finally {
                                lock.unlock();
                            }
                        });
                    }
                }

                /* if (i > maxIterations * .6 && RAND.nextDouble() < 0.05) {
                    System.out.printf("Iteration number %d: ", i);
                    flush();
                    break;
                } */
            }
            Platform.runLater(() -> {
                uiComponent.getScrnshotButton().setDisable(false);
                uiComponent.getToggle().setDisable(false);
                uiComponent.getAlgorithmSel().getSelectionModel().clearSelection();
                uiComponent.getAlgorithmSel().setManaged(true);
                uiComponent.getAlgorithmSel().setVisible(true);
                ((RadioButton) ((HBox) uiComponent.getVbox().getChildren().get(0)).getChildren().get(0)).setSelected(false);
                uiComponent.getVbox().setVisible(false);
                uiComponent.getVbox().setManaged(false);
                uiComponent.hideRunButton();
            });
            finishedRunning.set(true);
        }
        catch (InterruptedException e) {

        }
    }

    private void manualrun() {
        AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
        AppData dataComponent = ((AppData) applicationTemplate.getDataComponent());
        List<Double> xvalues = new ArrayList<>();
        dataComponent.getDataPoints().values().forEach(value -> xvalues.add(value.getX()));
        if (currentIteration < maxIterations && updateInterval <= maxIterations) {
            try {
                finishedRunning.set(false);
                currentIteration += updateInterval;
                Platform.runLater(() -> {
                    uiComponent.getScrnshotButton().setDisable(true);
                    uiComponent.getRunButton().setDisable(true);
                    uiComponent.getToggle().setDisable(true);
                    ((Button) ((HBox) uiComponent.getVbox().getChildren().get(0)).getChildren().get(1)).setDisable(true);
                });
                double xmin = Collections.min(xvalues);
                double xmax = Collections.max(xvalues);
                double yForXmin = getYValue(xmin);
                double yForXmax = getYValue(xmax);
                System.out.printf("Iteration number %d: ", currentIteration);
                flush();
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.getData().add(new XYChart.Data<>(xmin, yForXmin));
                series.getData().add(new XYChart.Data<>(xmax, yForXmax));
                String chartSeriesLine = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_SERIES_LINE.name());
                String strokeWidth = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES_STROKE_WIDTH.name());
                String chartLineSymbol = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_LINE_SYMBOL.name());
                String bgColor = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES_BG_COLOR.name());
                series.setName("Random Classifier Line");
                Platform.runLater(() -> {
                    lock.lock();
                    try {
                        if (uiComponent.getChart().getData().contains(prevSeriesRef.get()))
                            uiComponent.getChart().getData().remove(prevSeriesRef.get());
                        uiComponent.getChart().getData().add(series);
                        prevSeriesRef.set(series);
                        series.getNode().lookup(chartSeriesLine).setStyle(strokeWidth);
                        series.getData().forEach(data -> data.getNode().lookup(chartLineSymbol).setStyle(bgColor));
                    } finally {
                        lock.unlock();
                    }
                });
                Thread.sleep(500);
                if (currentIteration + updateInterval > maxIterations) { // on last iteration
                    Platform.runLater(() -> {
                        uiComponent.getScrnshotButton().setDisable(false);
                        uiComponent.getToggle().setDisable(false);
                        uiComponent.getAlgorithmSel().getSelectionModel().clearSelection();
                        uiComponent.getAlgorithmSel().setManaged(true);
                        uiComponent.getAlgorithmSel().setVisible(true);
                        ((RadioButton) ((HBox) uiComponent.getVbox().getChildren().get(0)).getChildren().get(0)).setSelected(false);
                        uiComponent.getVbox().setVisible(false);
                        uiComponent.getVbox().setManaged(false);
                        ((Button) ((HBox) uiComponent.getVbox().getChildren().get(0)).getChildren().get(1)).setDisable(false);
                        uiComponent.hideRunButton();
                    });
                    resetCurrentIteration();
                    finishedRunning.set(true);
                    return;
                } else {
                    Platform.runLater(() -> {
                        uiComponent.getScrnshotButton().setDisable(false);
                        uiComponent.getRunButton().setDisable(false);
                    });
                }
            } catch (InterruptedException e) {

            }
        }
    }

    // for internal viewing only
    protected void flush() {
        System.out.printf("%d\t%d\t%d%n", output.get(0), output.get(1), output.get(2));
    }

    protected void resetCurrentIteration() { currentIteration = 0; }

    private double getYValue(double xvalue) {
        int xCoefficient =  new Long(-1 * Math.round((2 * RAND.nextDouble() - 1) * 10)).intValue();
        int yCoefficient = 10;
        int constant     = RAND.nextInt(11);
        output = Arrays.asList(xCoefficient, yCoefficient, constant);
        return (constant - xCoefficient * xvalue) / yCoefficient;
    }

    @Override
    public boolean finishedRunning() { return finishedRunning.get(); }
}