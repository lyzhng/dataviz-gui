package algorithms;

import dataprocessors.AppData;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
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
    private static SimpleBooleanProperty isRunning = new SimpleBooleanProperty(false);
    ReentrantLock lock = new ReentrantLock();

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
            uiComponent.getRunButton().setDisable(true);
            List<Double> xvalues = new ArrayList<>();
            dataComponent.getDataPoints().values().forEach(value -> xvalues.add(value.getX()));
            double xmin = Collections.min(xvalues);
            double xmax = Collections.max(xvalues);
            for (int i = 1; i <= maxIterations && tocontinue(); i += 1) {
                isRunning.set(true);
                double yForXmin = getYValue(xmin);
                double yForXmax = getYValue(xmax);
                // everything below is just for internal viewing of how the output is changing
                // in the final project, such changes will be dynamically visible in the UI
                if (i % updateInterval == 0) {
                    System.out.printf("Iteration number %d: ", i);
                    flush();
                    XYChart.Series<Number, Number> series = new XYChart.Series<>();
                    series.setName("Regression");
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
                    Thread.sleep(750);
                    if (i + updateInterval <= maxIterations) {
                        AtomicInteger iter = new AtomicInteger(i);
                        Platform.runLater(() -> {
                            lock.lock();
                            try {
                                uiComponent.getChart().getData().remove(series);
                                // System.out.format("removing series %d + %d <= %d%n", iter.get(), updateInterval, maxIterations);
                            } finally {
                                lock.unlock();
                            }
                        });
                    } else { // last iteration
                        Platform.runLater(() -> {
                            uiComponent.getScrnshotButton().setDisable(false);
                        });
                    }
                }
                /* if (i > maxIterations * .6 && RAND.nextDouble() < 0.05) {
                    System.out.printf("Iteration number %d: ", i);
                    flush();
                    break;
                } */
            }
            isRunning.set(false);
        }
        catch (InterruptedException e) {
            System.out.println("Interrupted.");
        }
    }

    private void manualrun() {
        AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
        AppData dataComponent = ((AppData) applicationTemplate.getDataComponent());
        List<Double> xvalues = new ArrayList<>();
        dataComponent.getDataPoints().values().forEach(value -> xvalues.add(value.getX()));
        if (currentIteration < maxIterations) { // 9/2 -> 5
            try {
                isRunning.set(true);
                uiComponent.getRunButton().setDisable(true);
                double xmin = Collections.min(xvalues);
                double xmax = Collections.max(xvalues);
                double yForXmin = getYValue(xmin);
                double yForXmax = getYValue(xmax);
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.getData().add(new XYChart.Data<>(xmin, yForXmin));
                series.getData().add(new XYChart.Data<>(xmax, yForXmax));
                String chartSeriesLine = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_SERIES_LINE.name());
                String strokeWidth = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES_STROKE_WIDTH.name());
                String chartLineSymbol = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_LINE_SYMBOL.name());
                String bgColor = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES_BG_COLOR.name());
                series.setName("Regression");
                Platform.runLater(() -> {
                    lock.lock();
                    try {
                        if (uiComponent.getChart().getData().contains(prevSeriesRef.get())) uiComponent.getChart().getData().remove(prevSeriesRef.get());
                        uiComponent.getChart().getData().add(series);
                        prevSeriesRef.set(series);
                        series.getNode().lookup(chartSeriesLine).setStyle(strokeWidth);
                        series.getData().forEach(data -> data.getNode().lookup(chartLineSymbol).setStyle(bgColor));
                    } finally {
                        lock.unlock();
                    }
                });
                if (currentIteration == maxIterations - 1) { // on last iteration
                    uiComponent.getRunButton().setDisable(true);
                    resetCurrentIteration();
                    return;
                }
                currentIteration += updateInterval;
                Thread.sleep(750);
                isRunning.set(false);
                Platform.runLater(() -> {
                    uiComponent.getScrnshotButton().setDisable(false);
                });
                uiComponent.getRunButton().setDisable(false);
            } catch (InterruptedException e) {
                System.out.println("Manual run interrupted");
            }
        }
    }

    // for internal viewing only
    protected void flush() {
        System.out.printf("%d\t%d\t%d%n", output.get(0), output.get(1), output.get(2));
    }

    protected void resetCurrentIteration() { currentIteration = 0; }

    private double getYValue(double xvalue) {
        int xCoefficient = new Double(RAND.nextDouble() * 100).intValue();
        int yCoefficient = new Double(RAND.nextDouble() * 100).intValue();
        int constant = new Double(RAND.nextDouble() * 100).intValue();
        output = Arrays.asList(xCoefficient, yCoefficient, constant);
        return (constant - xCoefficient * xvalue) / yCoefficient;
    }

    public SimpleBooleanProperty isRunningProperty() { return isRunning; }
}