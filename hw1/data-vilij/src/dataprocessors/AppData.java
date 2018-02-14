package dataprocessors;

import settings.AppPropertyTypes;
import ui.AppUI;
import vilij.components.DataComponent;
import vilij.components.Dialog;
import vilij.settings.PropertyTypes;
import vilij.templates.ApplicationTemplate;

import java.nio.file.Path;

/**
 * This is the concrete application-specific implementation of the data component defined by the Vilij framework.
 *
 * @author Ritwik Banerjee
 * @see DataComponent
 */
public class AppData implements DataComponent {

    private TSDProcessor        processor; // the main data processor
    private ApplicationTemplate applicationTemplate;

    public AppData(ApplicationTemplate applicationTemplate) {
        this.processor = new TSDProcessor();
        this.applicationTemplate = applicationTemplate;
    }

    @Override
    public void loadData(Path dataFilePath) {
        // TODO: NOT A PART OF HW 1
    }

    public void loadData(String dataString) {
        // TODO for homework 1
        try {
            processor.processString(dataString);
        }
        catch (Exception e) {
            applicationTemplate
                    .getDialog(Dialog.DialogType.ERROR)
                    .show(applicationTemplate.manager.getPropertyValue(PropertyTypes.LOAD_ERROR_TITLE.name()),
                            applicationTemplate.manager.getPropertyValue(PropertyTypes.LOAD_ERROR_MSG.name()) +
                                    applicationTemplate.manager.getPropertyValue(AppPropertyTypes.TEXT_AREA.name()));

        }
    }

    @Override
    public void saveData(Path dataFilePath) {
        // TODO: NOT A PART OF HW 1
    }

    @Override
    public void clear() {
        processor.clear();
    }

    public void displayData() {
        processor.toChartData(((AppUI) applicationTemplate.getUIComponent()).getChart());
    }
}
