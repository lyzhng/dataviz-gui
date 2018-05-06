package dataprocessors;

import algorithms.Algorithm;
import algorithms.DataSet;
import algorithms.RandomClassifier;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import settings.AppPropertyTypes;
import ui.AppUI;
import ui.ConfigurationWindow;
import vilij.components.DataComponent;
import vilij.components.ErrorDialog;
import vilij.settings.PropertyTypes;
import vilij.templates.ApplicationTemplate;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
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
    Algorithm algorithm;


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
            }
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
        if (dataFilePath != null) {
            try (PrintWriter writer = new PrintWriter(Files.newOutputStream(dataFilePath))) {
                writer.write(((AppUI) applicationTemplate.getUIComponent()).getTextArea().getText());
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    @Override
    public void clear() {
        processor.clear();
    }

    public void displayData() {
        LineChart<Number, Number> chart = ((AppUI) applicationTemplate.getUIComponent()).getChart();
        chart.setAnimated(false);
        processor.toChartData(chart);
        String chartSeriesLine = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CHART_SERIES_LINE.name());
        String avgSeries = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.AVG_SERIES.name());
        String nullStroke = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.NULL_STROKE.name());
        chart.lookupAll(chartSeriesLine).forEach(node -> {
            if (!(node.lookup(avgSeries) == node))
                node.setStyle(nullStroke);
        });
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

    public Map<String, Point2D> getDataPoints() { return processor.getDataPoints(); }

    public AtomicBoolean hadAnError() { return processor.hadAnError; }

    public TSDProcessor getProcessor() { return processor; }

    public Map<String, String> getDataLabels() { return processor.getDataLabels(); }

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
        final String CLASSIFIER = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CLASSIFIER.name());
        final String CLUSTERER = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.CLUSTERER.name());
        AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
        ConfigurationWindow classificationWindow = uiComponent.getClassificationWindow();
        ConfigurationWindow clusteringWindow = uiComponent.getClusteringWindow();
        DataSet dataset = DataSet.fromTSDProcessor(uiComponent.getTextArea().getText());
        if (algorithm != null && algorithm.finishedRunning()) {
            uiComponent.clearChart();
            displayData();
        }
        uiComponent.setSelectedClusteringAlg(false);
        uiComponent.setSelectedClassificationAlg(false);
        try {
            clear();
            String text = uiComponent.getTextArea().getText();
            processor.processString(text);
            try {
                String filename = getAlgorithmFile();
                Class<?> clazz = Class.forName(filename);
                if (filename.contains(CLUSTERER)) {
                    Constructor<?> konstructor = clazz.getDeclaredConstructor(DataSet.class, ApplicationTemplate.class, int.class, int.class, boolean.class, int.class);
                    Algorithm algorithm = (Algorithm) (konstructor.newInstance(dataset, applicationTemplate, clusteringWindow.getMaxIter(), clusteringWindow.getUpdateInterval(), clusteringWindow.isContinuousRun(), clusteringWindow.getNumClusters()));
                    this.algorithm = algorithm;
                } else if (filename.contains(CLASSIFIER)) {
                    Constructor<?> konstructor = clazz.getDeclaredConstructor(DataSet.class, ApplicationTemplate.class, int.class, int.class, boolean.class);
                    Algorithm algorithm = (Algorithm) (konstructor.newInstance(dataset, applicationTemplate, classificationWindow.getMaxIter(), classificationWindow.getUpdateInterval(), classificationWindow.isContinuousRun()));
                    this.algorithm = algorithm;
                }
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) { }

            new Thread(algorithm).start();
        } catch (Exception e) { /* ignore */ }
    }

    public String getAlgorithmFile() {

        final String JAVA_EXT = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.JAVA_EXT.name());
        final String ALGORITHMS_REL_PATH = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.ALGORITHMS_REL_PATH.name());
        final String ALGORITHMS_REPLACE = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.ALGORITHMS_REPLACE.name());
        AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
        File file = new File(ALGORITHMS_REL_PATH);
        Map<String, String> fileMap = new LinkedHashMap<>();
        for (File f : file.listFiles()) {
            String filePath = f.toString().replace(ALGORITHMS_REPLACE, "").replace(JAVA_EXT, "").replace("/", ".");
            fileMap.put(f.getName().replace(JAVA_EXT, ""), filePath);
        }
        String algorithmName = "";
        if (((RadioButton) ((HBox) uiComponent.getVbox().getChildren().get(0)).getChildren().get(0)).isSelected()) { // random classifier is selected
            algorithmName = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.RANDOMCLASSIFIER.name()); // case needs to work for KMeansClusterer too
        } else if (((RadioButton) ((HBox) uiComponent.getVbox().getChildren().get(1)).getChildren().get(0)).isSelected()) { // random clustering is selected
            algorithmName = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.RANDOMCLUSTERER.name());
        } else if (((RadioButton) ((HBox) uiComponent.getVbox().getChildren().get(2)).getChildren().get(0)).isSelected()) {
            algorithmName = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.KMEANSCLUSTERER.name());
        }
        String name = "";
        for (String filename : fileMap.keySet()) {
            if (filename.equals(algorithmName)) {
                name = filename;
                break;
            }
        }
        return fileMap.get(name);
    }

    public Algorithm getAlgorithm() { return algorithm; }
}
