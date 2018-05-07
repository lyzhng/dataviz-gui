package algorithms;

import dataprocessors.AppData;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.HBox;
import settings.AppPropertyTypes;
import ui.AppUI;
import vilij.templates.ApplicationTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Ritwik Banerjee
 */
public class KMeansClusterer extends Clusterer {

    private DataSet dataset;
    private List<Point2D> centroids;

    private final int maxIterations;
    private final int updateInterval;
    private final AtomicBoolean tocontinue;
    private AtomicBoolean finishedRunning;
    private static int currentIteration = 0;
    ApplicationTemplate applicationTemplate;
    ReentrantLock lock;


    public KMeansClusterer(DataSet dataset, ApplicationTemplate applicationTemplate, int maxIterations, int updateInterval, boolean tocontinue, int numberOfClusters) {
        super(numberOfClusters);
        this.dataset = dataset;
        this.maxIterations = maxIterations;
        this.updateInterval = updateInterval;
        this.tocontinue = new AtomicBoolean(tocontinue);
        this.finishedRunning = new AtomicBoolean(true);
        this.applicationTemplate = applicationTemplate;
        lock = new ReentrantLock();
    }

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

    @Override
    public void run() {
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
            try {
                uiComponent.clearChart();
                ((AppData) applicationTemplate.getDataComponent()).getProcessor().processString(uiComponent.getTextArea().getText());
                ((AppData) applicationTemplate.getDataComponent()).getProcessor().toChartData(uiComponent.getChart());
                uiComponent.getChart().getData().forEach(ser -> {
                    ser.getNode().setStyle(applicationTemplate.manager.getPropertyValue(AppPropertyTypes.NULL_STROKE.name()));
                });
            } catch (Exception e) { }
        });
        initializeCentroids();
        if (tocontinue.get())
            continuousrun();
        else
            manualrun();
    }

    private void continuousrun() {
        AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
        AppData dataComponent = ((AppData) applicationTemplate.getDataComponent());
        finishedRunning.set(false);
        Platform.runLater(() -> {
            uiComponent.getRunButton().setDisable(true);
            uiComponent.getToggle().setDisable(true);
            uiComponent.getScrnshotButton().setDisable(true);
            (((HBox) uiComponent.getVbox().getChildren().get(2)).getChildren().get(1)).setDisable(true);
        });
        for (int i = 1; i <= maxIterations; i += 1) {
            if (i % updateInterval == 0) {
                assignLabels();
                recomputeCentroids();
                System.out.printf("Iteration number %d%n", i);
                Platform.runLater(() -> {
                    uiComponent.clearChart();
                    dataComponent.getProcessor().setDataLabels(dataset.getLabels());
                    dataComponent.getProcessor().setDataPoints(dataset.getLocations());
                    dataComponent.getProcessor().toChartData(uiComponent.getChart());
                    uiComponent.getChart().getData().forEach(ser -> {
                        ser.getNode().setStyle(applicationTemplate.manager.getPropertyValue(AppPropertyTypes.NULL_STROKE.name()));
                    });
                });
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) { /* do nothing */ }
            }
        }
        Platform.runLater(() -> {
            uiComponent.getScrnshotButton().setDisable(false);
            uiComponent.getToggle().setDisable(false);
            uiComponent.getAlgorithmSel().getSelectionModel().clearSelection();
            uiComponent.getAlgorithmSel().setManaged(true);
            uiComponent.getAlgorithmSel().setVisible(true);
            ((RadioButton) ((HBox) uiComponent.getVbox().getChildren().get(2)).getChildren().get(0)).setSelected(false);
            ((Button) ((HBox) uiComponent.getVbox().getChildren().get(2)).getChildren().get(1)).setDisable(false);
            uiComponent.getVbox().setVisible(false);
            uiComponent.getVbox().setManaged(false);
            uiComponent.hideRunButton();
        });
        finishedRunning.set(true);
    }

    private void initializeCentroids() {
        Set<String> chosen = new HashSet<>();
        List<String> instanceNames = new ArrayList<>(dataset.getLabels().keySet());
        Random r = new Random();
        while (chosen.size() < numberOfClusters) {
            int i = r.nextInt(instanceNames.size());
            while (chosen.contains(instanceNames.get(i)))
                i = (++i % instanceNames.size());
            chosen.add(instanceNames.get(i));
        }
        centroids = chosen.stream().map(name -> dataset.getLocations().get(name)).collect(Collectors.toList());
    }

    private void assignLabels() {
        dataset.getLocations().forEach((instanceName, location) -> {
            double minDistance = Double.MAX_VALUE;
            int minDistanceIndex = -1;
            for (int i = 0; i < centroids.size(); i++) {
                double distance = computeDistance(centroids.get(i), location);
                if (distance < minDistance) {
                    minDistance = distance;
                    minDistanceIndex = i;
                }
            }
            dataset.getLabels().put(instanceName, Integer.toString(minDistanceIndex));
        });
    }

    private void recomputeCentroids() {
        IntStream.range(0, numberOfClusters).forEach(i -> {
            AtomicInteger clusterSize = new AtomicInteger();
            Point2D sum = dataset.getLabels()
                    .entrySet()
                    .stream()
                    .filter(entry -> i == Integer.parseInt(entry.getValue()))
                    .map(entry -> dataset.getLocations().get(entry.getKey()))
                    .reduce(new Point2D(0, 0), (p, q) -> {
                        clusterSize.incrementAndGet();
                        return new Point2D(p.getX() + q.getX(), p.getY() + q.getY());
                    });
            Point2D newCentroid = new Point2D(sum.getX() / clusterSize.get(), sum.getY() / clusterSize.get());
            if (!newCentroid.equals(centroids.get(i))) {
                centroids.set(i, newCentroid);
            }
        });
    }

    private void manualrun() {
        AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
        AppData dataComponent = ((AppData) applicationTemplate.getDataComponent());
        if (currentIteration < maxIterations && updateInterval <= maxIterations) {
            try {
                assignLabels();
                recomputeCentroids();
                finishedRunning.set(false);
                currentIteration += updateInterval;
                Platform.runLater(() -> {
                    uiComponent.getScrnshotButton().setDisable(true);
                    uiComponent.getRunButton().setDisable(true);
                    uiComponent.getToggle().setDisable(true);
                    ((Button) ((HBox) uiComponent.getVbox().getChildren().get(2)).getChildren().get(1)).setDisable(true);
                });
                System.out.printf("Iteration number %d%n", currentIteration);
                Platform.runLater(() -> {
                    uiComponent.clearChart();
                    dataComponent.getProcessor().setDataLabels(dataset.getLabels());
                    dataComponent.getProcessor().setDataPoints(dataset.getLocations());
                    dataComponent.getProcessor().toChartData(uiComponent.getChart());
                    uiComponent.getChart().getData().forEach(ser -> {
                        ser.getNode().setStyle(applicationTemplate.manager.getPropertyValue(AppPropertyTypes.NULL_STROKE.name()));
                    });
                });
                Thread.sleep(500);
                if (currentIteration + updateInterval > maxIterations) { // on last iteration
                    Platform.runLater(() -> {
                        uiComponent.getScrnshotButton().setDisable(false);
                        uiComponent.getToggle().setDisable(false);
                        uiComponent.getAlgorithmSel().getSelectionModel().clearSelection();
                        uiComponent.getAlgorithmSel().setManaged(true);
                        uiComponent.getAlgorithmSel().setVisible(true);
                        ((RadioButton) ((HBox) uiComponent.getVbox().getChildren().get(2)).getChildren().get(0)).setSelected(false);
                        uiComponent.getVbox().setVisible(false);
                        uiComponent.getVbox().setManaged(false);
                        ((Button) ((HBox) uiComponent.getVbox().getChildren().get(2)).getChildren().get(1)).setDisable(false);
                        uiComponent.hideRunButton();
                    });
                    currentIteration = 0;
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

    private static double computeDistance(Point2D p, Point2D q) {
        return Math.sqrt(Math.pow(p.getX() - q.getX(), 2) + Math.pow(p.getY() - q.getY(), 2));
    }

    @Override
    public boolean finishedRunning() {
        return finishedRunning.get();
    }

}