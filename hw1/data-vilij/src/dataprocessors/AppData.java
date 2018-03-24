package dataprocessors;

import javafx.geometry.Point2D;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.control.TextArea;
import settings.AppPropertyTypes;
import ui.AppUI;
import vilij.components.DataComponent;
import vilij.components.Dialog;
import vilij.components.ErrorDialog;
import vilij.propertymanager.PropertyManager;
import vilij.settings.PropertyTypes;
import vilij.templates.ApplicationTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * This is the concrete application-specific implementation of the data component defined by the Vilij framework.
 *
 * @author Ritwik Banerjee
 * @see DataComponent
 */
public class AppData implements DataComponent {

    private TSDProcessor        processor;
    private ApplicationTemplate applicationTemplate;
    protected Boolean hadAnError;
    private List<String> names;
    private Integer indexDup;
    private List<String> lines; //


    public AppData(ApplicationTemplate applicationTemplate) {
        this.processor = new TSDProcessor();
        this.applicationTemplate = applicationTemplate;
        hadAnError = false;
        names = new ArrayList<>();
        indexDup = -1;
        lines = new ArrayList<>();
    }

    @Override
    public void loadData(Path dataFilePath) {
//        clear();
        int lineCounter = 0;
        boolean moreThanTen = false;
        try {
            TextArea textArea = ((AppUI) applicationTemplate.getUIComponent()).getTextArea();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(dataFilePath.toFile()));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
                lineCounter++;
                if (lineCounter == 10) {
                    textArea.setText(stringBuilder.toString());
                    moreThanTen = true;
                }
            }
            if (moreThanTen) {
                ErrorDialog errorDialog = ErrorDialog.getDialog();
                String errDialogTitle = "Load Warning";
                String errDialogMsg = String.format("Loaded data consists of %s lines. Showing only the first 10 in the text area.", lineCounter);
                errorDialog.show(errDialogTitle, errDialogMsg);
                setTextAreaActions();
            } else {
                textArea.setText(stringBuilder.toString());
            }
            loadData(stringBuilder.toString());
            displayData();
        }
        catch (Exception e) {
            if (!hadAnError) {
                String errLoadingTitle = applicationTemplate.manager.getPropertyValue(PropertyTypes.LOAD_ERROR_TITLE.name());
                String errLoadingMsg = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.ERR_LOADING_TXT.name());
                ErrorDialog errorDialog = ErrorDialog.getDialog();
                errorDialog.show(errLoadingTitle, errLoadingMsg + lineCounter);
            } else {
                ErrorDialog     dialog   = (ErrorDialog) applicationTemplate.getDialog(Dialog.DialogType.ERROR);
                PropertyManager manager  = applicationTemplate.manager;
                String errTitle = manager.getPropertyValue(PropertyTypes.LOAD_ERROR_TITLE.name());
                String duplicateMsg = manager.getPropertyValue(AppPropertyTypes.DUPLICATE_ERR_MSG.name());
                String duplicateMsgCont = lines.get(indexDup);
                dialog.show(errTitle, duplicateMsg + duplicateMsgCont);
            }
        }
    }

    public void loadData(String dataString) {
        try {
            processor.processString(dataString);
            checkForDuplicates(dataString);
            if (indexDup == -1)
                hadAnError = false;
            else
                hadAnError = true;

            if (hadAnError) {
//                ErrorDialog     dialog   = (ErrorDialog) applicationTemplate.getDialog(Dialog.DialogType.ERROR);
//                PropertyManager manager  = applicationTemplate.manager;
//                String errTitle = manager.getPropertyValue(PropertyTypes.LOAD_ERROR_TITLE.name());
//                String duplicateMsg = manager.getPropertyValue(AppPropertyTypes.DUPLICATE_ERR_MSG.name());
//                String duplicateMsgCont = lines.get(indexDup);
//                dialog.show(errTitle, duplicateMsg + duplicateMsgCont);
                throw new Exception();
            }
        } catch (Exception e) {
            // fixme
            ErrorDialog     dialog   = (ErrorDialog) applicationTemplate.getDialog(Dialog.DialogType.ERROR);
            PropertyManager manager  = applicationTemplate.manager;
            String          errTitle = manager.getPropertyValue(PropertyTypes.LOAD_ERROR_TITLE.name());
            String          errMsg   = manager.getPropertyValue(PropertyTypes.LOAD_ERROR_MSG.name());
            String          errInput = manager.getPropertyValue(AppPropertyTypes.TEXT_AREA.name());
            String  errMsgCont =
                    manager.getPropertyValue(AppPropertyTypes.OCCURRED_AT.name()) +
                    e.getMessage().substring(e.getMessage().indexOf("\n") + 1);
            dialog.show(errTitle, errMsg + errInput + "\n" + errMsgCont);
            clear();
            ((AppUI) applicationTemplate.getUIComponent()).clearChart();
            hadAnError = true;
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
        names.clear();
        lines = new ArrayList<>();
        indexDup = -1;
        hadAnError = false;
    }

    public void displayData() {
        LineChart<Number, Number> chart = ((AppUI) applicationTemplate.getUIComponent()).getChart();
        processor.toChartData(chart);
        //  make lines invisible here
        chart.lookupAll(".chart-series-line").stream().forEach(node -> {
           node.setStyle("-fx-stroke:null;");
        });
        ((AppUI) applicationTemplate.getUIComponent()).setTooltips();
    }

    public Boolean getHadAnError() {
        return hadAnError;
    }

    public void checkForDuplicates(String tsdString) {
        // FIXME
        indexDup = -1;
        String[] linesArr = tsdString.split("\n");
        lines = Arrays.asList(linesArr);
        try {
          Stream.of(tsdString.split("\n")).map(line -> Arrays.asList(line.split("\t"))).forEach(list -> {
              String name = list.get(0);
              names.add(name);
          });
          for (int i = 0; i < names.size(); i++) {
              for (int j = i+1; j < names.size(); j++) {
                  if (names.get(i).equals(names.get(j))) {
                      indexDup = i;
                      break;
                  }
              }
          }
//          names.clear();
        }
        catch (Exception e) {

        }
    }

    /**
     * Helper method for loadData.
     * When there are fewer than 10 lines in oldValue,
     * the newValue should add on to the text area.
     * FIXME
     */
    private void setTextAreaActions() {
        AtomicInteger index = new AtomicInteger(10);
        TextArea textArea = ((AppUI) applicationTemplate.getUIComponent()).getTextArea();
        textArea.textProperty().addListener(((observable, oldValue, newValue) -> {
            String[] prev = oldValue.split("\n");
            if (prev.length < 10 && lines.size() > 10) {
                if (index.get() >= lines.size()) {
                    return;
                }
                Optional<String> curLine = Optional.of(String.valueOf(lines.get(index.get())));
                if (curLine.isPresent()) {
                    if (index.get() == 10) {
                        textArea.setText(oldValue + curLine.get());
                    }
                    else {
                        textArea.setText(oldValue + "\n" + curLine.get());
                    }
                    index.getAndIncrement();
                }
            }
        }));
    }

    public LinkedHashMap<String, Point2D> getDataPoints() {
        return processor.getDataPoints();
    }

    public LinkedHashMap<String, String> getDataLabels() {
        return processor.getDataLabels();
    }

}
