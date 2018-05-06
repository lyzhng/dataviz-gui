package algorithms;

import dataprocessors.AppData;
import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.HBox;
import ui.AppUI;
import vilij.templates.ApplicationTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class RandomClusterer extends Clusterer {

    private DataSet dataset;
    private final int maxIterations;
    private final int updateInterval;
    private final AtomicBoolean tocontinue;
    private AtomicBoolean finishedRunning;
    private static int currentIteration = 0;
    ApplicationTemplate applicationTemplate;
    ReentrantLock lock;

    public RandomClusterer(DataSet dataset, ApplicationTemplate applicationTemplate, int maxIterations, int updateInterval, boolean tocontinue, int numberOfClusters) {
        super(numberOfClusters);
        this.dataset = dataset;
        this.maxIterations = maxIterations;
        this.updateInterval = updateInterval;
        this.tocontinue = new AtomicBoolean(tocontinue);
        this.finishedRunning = new AtomicBoolean(true);
        this.applicationTemplate = applicationTemplate;
        this.lock = new ReentrantLock();
    }

    public String generatedLabel() {
        return String.valueOf((int) ((Math.random() * numberOfClusters)));
    }

    public DataSet newLabels() {
        List<String> names = new ArrayList<>(dataset.getLabels().keySet());
        Random random = new Random();
        int iteration = 0;
        while (iteration < maxIterations) {
            int randomInt = random.nextInt(names.size());
            if (!dataset.getLabels().get(names.get(randomInt)).equals("null")) {
                dataset.updateLabel(names.get(randomInt), generatedLabel());
            }
            iteration++;
        }
        return dataset;
    }

    @Override
    public int getMaxIterations() { return maxIterations; }

    @Override
    public int getUpdateInterval() { return updateInterval; }

    @Override
    public boolean tocontinue() { return tocontinue.get(); }

    @Override
    public boolean finishedRunning() { return finishedRunning.get(); }

    @Override
    public void run() {
        Platform.setImplicitExit(false);
        if (tocontinue.get()) {
            continuousrun();
        } else {
            manualrun();
        }
    }

    private void manualrun() {
        AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
        AppData dataComponent = ((AppData) applicationTemplate.getDataComponent());
        if (currentIteration < maxIterations && updateInterval <= maxIterations) {
            try {
                finishedRunning.set(false);
                currentIteration += updateInterval;
                Platform.runLater(() -> {
                    uiComponent.getScrnshotButton().setDisable(true);
                    uiComponent.getRunButton().setDisable(true);
                    uiComponent.getToggle().setDisable(true);
                    ((Button) ((HBox) uiComponent.getVbox().getChildren().get(1)).getChildren().get(1)).setDisable(true);
                });
                System.out.printf("Iteration number %d%n", currentIteration);
                DataSet updatedData = newLabels();
                Platform.runLater(() -> {
                    lock.lock();
                    try {
                        uiComponent.clearChart();
                        dataComponent.getProcessor().setDataLabels(updatedData.getLabels());
                        dataComponent.getProcessor().setDataPoints(updatedData.getLocations());
                        dataComponent.getProcessor().toChartData(uiComponent.getChart());
                        uiComponent.getChart().getData().forEach(ser -> {
                            ser.getNode().setStyle("-fx-stroke: null");
                        });
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
                        ((RadioButton) ((HBox) uiComponent.getVbox().getChildren().get(1)).getChildren().get(0)).setSelected(false);
                        uiComponent.getVbox().setVisible(false);
                        uiComponent.getVbox().setManaged(false);
                        ((Button) ((HBox) uiComponent.getVbox().getChildren().get(1)).getChildren().get(1)).setDisable(false);
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
            } catch (InterruptedException e) { }
        }
    }

    private void continuousrun() {
        try {
            AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
            AppData dataComponent = ((AppData) applicationTemplate.getDataComponent());
            finishedRunning.set(false);
            for (int i = 1; i <= maxIterations && tocontinue(); i += 1) {
                Platform.runLater(() -> {
                    uiComponent.getRunButton().setDisable(true);
                    uiComponent.getToggle().setDisable(true);
                    uiComponent.getScrnshotButton().setDisable(true);
                    ((Button) ((HBox) uiComponent.getVbox().getChildren().get(0)).getChildren().get(1)).setDisable(true);
                });
                // everything below is just for internal viewing of how the output is changing
                // in the final project, such changes will be dynamically visible in the UI
                if (i % updateInterval == 0) {
                    System.out.printf("Iteration number %d%n", i);
                    DataSet updatedDataSet = newLabels();
                    Platform.runLater(() -> {
                        lock.lock();
                        try {
                            uiComponent.clearChart();
                            dataComponent.getProcessor().setDataLabels(updatedDataSet.getLabels());
                            dataComponent.getProcessor().setDataPoints(updatedDataSet.getLocations());
                            dataComponent.getProcessor().toChartData(uiComponent.getChart());
                            uiComponent.getChart().getData().forEach(ser -> {
                                ser.getNode().setStyle("-fx-stroke: null");
                            });
                        } finally {
                            lock.unlock();
                        }
                    });
                    Thread.sleep(500);
                }
            }
            Platform.runLater(() -> {
                uiComponent.getScrnshotButton().setDisable(false);
                uiComponent.getToggle().setDisable(false);
                uiComponent.getAlgorithmSel().getSelectionModel().clearSelection();
                uiComponent.getAlgorithmSel().setManaged(true);
                uiComponent.getAlgorithmSel().setVisible(true);
                ((RadioButton) ((HBox) uiComponent.getVbox().getChildren().get(0)).getChildren().get(0)).setSelected(false);
                ((Button) ((HBox) uiComponent.getVbox().getChildren().get(0)).getChildren().get(1)).setDisable(false);
                uiComponent.getVbox().setVisible(false);
                uiComponent.getVbox().setManaged(false);
                uiComponent.hideRunButton();
            });
            finishedRunning.set(true);
        }
        catch (InterruptedException e) { }
    }
}
