package algorithms;

import dataprocessors.AppData;
import javafx.application.Platform;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import settings.AppPropertyTypes;
import ui.AppUI;
import vilij.settings.PropertyTypes;
import vilij.templates.ApplicationTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
        if (tocontinue())
            continuousrun();
        else
            manualrun();
    }

    public void continuousrun() {
        try {
            AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
            AppData dataComponent = ((AppData) applicationTemplate.getDataComponent());
            uiComponent.getRunButton().setDisable(true);
            List<Double> xvalues = new ArrayList<>();
            dataComponent.getDataPoints().values().forEach(value -> xvalues.add(value.getX()));
            for (int i = 1; i <= maxIterations && tocontinue(); i++) {
                int xCoefficient = new Double(RAND.nextDouble() * 100).intValue();
                int yCoefficient = new Double(RAND.nextDouble() * 100).intValue();
                int constant = new Double(RAND.nextDouble() * 100).intValue();
                // this is the real output of the classifier
                output = Arrays.asList(xCoefficient, yCoefficient, constant);
                // everything below is just for internal viewing of how the output is changing
                // in the final project, such changes will be dynamically visible in the UI
                if (i % updateInterval == 0) {
                    System.out.printf("Iteration number %d: ", i);
                    flush();
                }
                if (i > maxIterations * .6 && RAND.nextDouble() < 0.05) {
                    System.out.printf("Iteration number %d: ", i);
                    flush();
                    break;
                }
                double xmin = Collections.min(xvalues);
                double xmax = Collections.max(xvalues);
                double yForXmin = (constant - xCoefficient * xmin) / yCoefficient;
                double yForXmax = (constant - xCoefficient * xmax) / yCoefficient;
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName("Regression");
                Platform.runLater(() -> {
                    series.getData().add(new XYChart.Data<>(xmin, yForXmin));
                    series.getData().add(new XYChart.Data<>(xmax, yForXmax));
                    uiComponent.getChart().getData().add(series);
                    String chartSeriesLine = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_SERIES_LINE.name());
                    String strokeWidth = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES_STROKE_WIDTH.name());
                    String chartLineSymbol = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_LINE_SYMBOL.name());
                    String bgColor = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES_BG_COLOR.name());
                    series.getNode().lookup(chartSeriesLine).setStyle(strokeWidth);
                    series.getData().forEach(data -> data.getNode().lookup(chartLineSymbol).setStyle(bgColor));

                });
                Thread.sleep(750);
                if (i != maxIterations) {
                    Platform.runLater(() -> {
                        uiComponent.getChart().getData().remove(series);
                    });
                }
            }
        }
        catch (InterruptedException e) {
            System.out.println("Interrupted.");
        }
    }

    public void manualrun() {
        AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
        AppData dataComponent = ((AppData) applicationTemplate.getDataComponent());
        List<Double> xvalues = new ArrayList<>();
        dataComponent.getDataPoints().values().forEach(value -> xvalues.add(value.getX()));
        if (currentIteration < maxIterations) {
            try {
                // math math math
                int xCoefficient = new Double(RAND.nextDouble() * 100).intValue();
                int yCoefficient = new Double(RAND.nextDouble() * 100).intValue();
                int constant = new Double(RAND.nextDouble() * 100).intValue();
                output = Arrays.asList(xCoefficient, yCoefficient, constant);
                double xmin = Collections.min(xvalues);
                double xmax = Collections.max(xvalues);
                double yForXmin = (constant - xCoefficient * xmin) / yCoefficient;
                double yForXmax = (constant - xCoefficient * xmax) / yCoefficient;
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName("Regression");
                Platform.runLater(() -> {
                    if (uiComponent.getChart().getData().contains(prevSeriesRef.get()))
                        uiComponent.getChart().getData().remove(prevSeriesRef.get());
                    series.getData().add(new XYChart.Data<>(xmin, yForXmin));
                    series.getData().add(new XYChart.Data<>(xmax, yForXmax));
                    uiComponent.getChart().getData().add(series);
                    prevSeriesRef.set(series);
                    String chartSeriesLine = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_SERIES_LINE.name());
                    String strokeWidth = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES_STROKE_WIDTH.name());
                    String chartLineSymbol = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_LINE_SYMBOL.name());
                    String bgColor = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES_BG_COLOR.name());
                    series.getNode().lookup(chartSeriesLine).setStyle(strokeWidth);
                    series.getData().forEach(data -> data.getNode().lookup(chartLineSymbol).setStyle(bgColor));
                });
                if (currentIteration == maxIterations - 1) { // on last iteration
                    uiComponent.getRunButton().setDisable(true);
                    resetCurrentIteration();
                    return;
                }
                currentIteration += updateInterval;
                Thread.sleep(750);
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

    /** A placeholder main method to just make sure this code runs smoothly */
    /* public static void main(String... args) throws IOException {
        DataSet          dataset    = DataSet.fromTSDFile(new File("/Users/lilyzhong/IdeaProjects/cse219hw2/hw1/data-vilij/resources/data/data1.tsd").toPath());
        RandomClassifier classifier = new RandomClassifier(dataset, 100, 5, true);
        classifier.run(); // no multithreading yet
    } */
}