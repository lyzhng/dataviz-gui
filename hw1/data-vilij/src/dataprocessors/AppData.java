package dataprocessors;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    protected List<String> names;
    protected Integer indexDup;
    protected List<String> lines;

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
        int lineCounter = 1;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(dataFilePath.toFile()));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
                lineCounter++;
            }
            ((AppUI) applicationTemplate.getUIComponent()).getTextArea().setText(stringBuilder.toString());
            loadData(stringBuilder.toString());
            displayData();
        }
        catch (Exception e) {
            String errLoadingTitle = applicationTemplate.manager.getPropertyValue(PropertyTypes.LOAD_ERROR_TITLE.name());
            String errLoadingMsg = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.ERR_LOADING_TXT.name());
            ErrorDialog errorDialog = ErrorDialog.getDialog();
            errorDialog.show(errLoadingTitle,errLoadingMsg + lineCounter);
        }
    }

    public void loadData(String dataString) {
        try {
            checkForDuplicates(dataString);
            if (indexDup == -1) {
                processor.processString(dataString);
                hadAnError = false;
            }
            else {
                ErrorDialog     dialog   = (ErrorDialog) applicationTemplate.getDialog(Dialog.DialogType.ERROR);
                PropertyManager manager  = applicationTemplate.manager;
                String errTitle = manager.getPropertyValue(PropertyTypes.LOAD_ERROR_TITLE.name());
                String duplicateMsg = manager.getPropertyValue(AppPropertyTypes.DUPLICATE_ERR_MSG.name());
                String duplicateMsgCont = lines.get(indexDup);
                dialog.show(errTitle, duplicateMsg + duplicateMsgCont);
                clear();
                hadAnError = true;
            }
        } catch (Exception e) {
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
    }

    public void displayData() {
        processor.toChartData(((AppUI) applicationTemplate.getUIComponent()).getChart());
    }

    public Boolean getHadAnError() {
        return hadAnError;
    }

    public void checkForDuplicates(String tsdString) {
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
                  break;
              }
          }
          names.clear();
        }
        catch (Exception e) {

        }
    }

}
