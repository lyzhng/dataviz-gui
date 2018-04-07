package actions;

import dataprocessors.AppData;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.chart.LineChart;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import settings.AppPropertyTypes;
import ui.AppUI;
import vilij.components.ActionComponent;
import vilij.components.ConfirmationDialog;
import vilij.components.Dialog;
import vilij.components.ErrorDialog;
import vilij.propertymanager.PropertyManager;
import vilij.settings.PropertyTypes;
import vilij.templates.ApplicationTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.io.File.separator;
import static vilij.settings.PropertyTypes.SAVE_WORK_TITLE;

/**
 * This is the concrete implementation of the action handlers required by the application.
 *
 * @author Ritwik Banerjee
 */
public final class AppActions implements ActionComponent {

    /** The application to which this class of actions belongs. */
    private ApplicationTemplate applicationTemplate;

    /** Path to the data file currently active. */
    Path dataFilePath;

    /** The boolean property marking whether or not there are any unsaved changes. */
    SimpleBooleanProperty isUnsaved;

    public AppActions(ApplicationTemplate applicationTemplate) {
        this.applicationTemplate = applicationTemplate;
        this.isUnsaved = new SimpleBooleanProperty(false);
    }

    public void setIsUnsavedProperty(boolean property) { isUnsaved.set(property); }

    @Override
    public void handleNewRequest() {
        try {
            if (!isUnsaved.get() || promptToSave()) {
                AppUI appUI = ((AppUI) applicationTemplate.getUIComponent());
                applicationTemplate.getDataComponent().clear();
                applicationTemplate.getUIComponent().clear();
                isUnsaved.set(false);
                dataFilePath = null;
                enableTextArea();
                showTextArea();
                showToggles();
            }
        } catch (IOException e) { errorHandlingHelper(); }
    }

    @Override
    public void handleSaveRequest() {
        try {
            AtomicBoolean hadAnError = ((AppData) applicationTemplate.getDataComponent()).hadAnError();
            if (isSaved() && !hadAnError.get()) {
                save();
            } else if (!isSaved() && !hadAnError.get()){
                promptToSave();
                setIsUnsavedProperty(false);
            } else {
                String errSaveTitle = applicationTemplate.manager.getPropertyValue(PropertyTypes.SAVE_ERROR_TITLE.name());
                String errSaveMsg = applicationTemplate.manager.getPropertyValue(PropertyTypes.SAVE_ERROR_MSG.name());
                String errSaveMsgCont = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.DATA_FILE_LABEL.name());
                ErrorDialog errorDialog = ErrorDialog.getDialog();
                errorDialog.show(errSaveTitle, errSaveMsg + errSaveMsgCont);
            }

            if (isSaved() || hadAnError.get()) {
                ((AppUI) (applicationTemplate.getUIComponent())).getSaveButton().setDisable(true);
            }

        } catch (IOException e) {
            String errSaveTitle = applicationTemplate.manager.getPropertyValue(PropertyTypes.SAVE_ERROR_TITLE.name());
            String errSaveMsg = applicationTemplate.manager.getPropertyValue(PropertyTypes.SAVE_ERROR_MSG.name());
            String errSaveMsgCont = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.DATA_FILE_LABEL.name());
            ErrorDialog errorDialog = ErrorDialog.getDialog();
            errorDialog.show(errSaveTitle, errSaveMsg + errSaveMsgCont);
        }
    }

    @Override
    public void handleLoadRequest() {
        try {
            ((AppUI) (applicationTemplate.getUIComponent())).clearChart();
            AppData dataComponent = (AppData) applicationTemplate.getDataComponent();
            dataComponent.clear();
            FileChooser fileChooser = new FileChooser();
            // FIXME: Not Generic
            FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Tab-Separated Data File (.*.tsd)", "*.tsd");
            fileChooser.getExtensionFilters().add(extensionFilter);
            String dataDirPath = separator + applicationTemplate.manager.getPropertyValue(AppPropertyTypes.DATA_RESOURCE_PATH.name());
            URL dataDirURL = getClass().getResource(dataDirPath);
            fileChooser.setInitialDirectory(new File(dataDirURL.getFile()));
            File selected = fileChooser.showOpenDialog(applicationTemplate.getUIComponent().getPrimaryWindow());
            if (selected != null) {
                applicationTemplate.getDataComponent().loadData(selected.toPath());
                showTextArea();
                if (!dataComponent.hadAnError().get()) showStatsAndAlgorithm();
                else hideStatsAndAlgorithm();
                hideToggles();
            }
        } catch (Exception e) {
            String errLoadTitle = applicationTemplate.manager.getPropertyValue(PropertyTypes.LOAD_ERROR_TITLE.name());
            String errLoadMsg = applicationTemplate.manager.getPropertyValue(PropertyTypes.LOAD_ERROR_MSG.name());
            String errLoadMsgCont = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.DATA_FILE_LABEL.name());
            ErrorDialog errorDialog = ErrorDialog.getDialog();
            errorDialog.show(errLoadTitle, errLoadMsg + errLoadMsgCont);
        }
    }

    public void showTextArea() {
        AppUI appUI = ((AppUI) applicationTemplate.getUIComponent());
        appUI.getTextArea().setVisible(true);
        appUI.getTextArea().setManaged(true);
    }

    public void hideTextArea() {
        AppUI appUI = ((AppUI) applicationTemplate.getUIComponent());
        appUI.getTextArea().setManaged(false);
        appUI.getTextArea().setVisible(false);
    }

    public void showStatsAndAlgorithm() {
        AppUI appUI = ((AppUI) applicationTemplate.getUIComponent());
        appUI.getStatsText().setVisible(true);
        appUI.getStatsText().setManaged(true);
        appUI.getAlgorithmSel().setVisible(true);
        appUI.getAlgorithmSel().setManaged(true);
    }

    public void hideStatsAndAlgorithm() {
        AppUI appUI = ((AppUI) applicationTemplate.getUIComponent());
        appUI.getStatsText().setVisible(false);
        appUI.getStatsText().setManaged(false);
        appUI.getAlgorithmSel().setVisible(false);
        appUI.getAlgorithmSel().setManaged(false);
    }

    public void showToggles() {
        AppUI appUI = ((AppUI) applicationTemplate.getUIComponent());
        appUI.getToggleButton().setVisible(true);
        appUI.getToggleButton().setManaged(true);
    }

    public void enableTextArea() {
        AppUI appUI = ((AppUI) applicationTemplate.getUIComponent());
        appUI.getTextArea().setDisable(false);
    }

    public void disableTextArea() {
        AppUI appUI = ((AppUI) applicationTemplate.getUIComponent());
        appUI.getTextArea().setDisable(true);
    }

    public void hideToggles() {
        AppUI appUI = ((AppUI) applicationTemplate.getUIComponent());
        appUI.getToggleButton().setVisible(false);
        appUI.getToggleButton().setManaged(false);
    }

    @Override
    public void handleExitRequest() {
        try {
            if (!isUnsaved.get() || promptToSave())
                System.exit(0);
        } catch (IOException e) { errorHandlingHelper(); }
    }

    @Override
    public void handlePrintRequest() {

    }

    public void handleScreenshotRequest() {
        LineChart<Number, Number> chart = ((AppUI) applicationTemplate.getUIComponent()).getChart();
        chart.setAnimated(false);
        WritableImage image = chart.snapshot(null,null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

        // setting up filechooser to select location for saved image
        FileChooser fileChooser = new FileChooser();
        String      dataDirPath = separator + applicationTemplate.manager.getPropertyValue(AppPropertyTypes.DATA_RESOURCE_PATH.name());
        URL         dataDirURL  = getClass().getResource(dataDirPath);
        fileChooser.setInitialDirectory(new File(dataDirURL.getFile()));
        fileChooser.setTitle(applicationTemplate.manager.getPropertyValue(SAVE_WORK_TITLE.name()));
        File location = fileChooser.showSaveDialog(applicationTemplate.getUIComponent().getPrimaryWindow());

        try {
            String ext = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.PNG_EXT.name());
            if (bufferedImage != null && location != null)
                ImageIO.write(bufferedImage, ext, location);

        } catch (IOException e) {
            errorHandlingHelper();
        }
    }

    /**
     * This helper method verifies that the user really wants to save their unsaved work, which they might not want to
     * do. The user will be presented with three options:
     * <ol>
     * <li><code>yes</code>, indicating that the user wants to save the work and continue with the action,</li>
     * <li><code>no</code>, indicating that the user wants to continue with the action without saving the work, and</li>
     * <li><code>cancel</code>, to indicate that the user does not want to continue with the action, but also does not
     * want to save the work at this point.</li>
     * </ol>
     *
     * @return <code>false</code> if the user presses the <i>cancel</i>, and <code>true</code> otherwise.
     */
    private boolean promptToSave() throws IOException {
        PropertyManager    manager = applicationTemplate.manager;
        ConfirmationDialog dialog  = ConfirmationDialog.getDialog();
        dialog.show(manager.getPropertyValue(AppPropertyTypes.SAVE_UNSAVED_WORK_TITLE.name()),
                    manager.getPropertyValue(AppPropertyTypes.SAVE_UNSAVED_WORK.name()));

        if (dialog.getSelectedOption() == null) return false; // if user closes dialog using the window's close button

        if (dialog.getSelectedOption().equals(ConfirmationDialog.Option.YES)) {
            if (dataFilePath == null) {
                FileChooser fileChooser = new FileChooser();
                String      dataDirPath = separator + manager.getPropertyValue(AppPropertyTypes.DATA_RESOURCE_PATH.name());
                URL         dataDirURL  = getClass().getResource(dataDirPath);

                if (dataDirURL == null)
                    throw new FileNotFoundException(manager.getPropertyValue(AppPropertyTypes.RESOURCE_SUBDIR_NOT_FOUND.name()));

                fileChooser.setInitialDirectory(new File(dataDirURL.getFile()));
                fileChooser.setTitle(manager.getPropertyValue(SAVE_WORK_TITLE.name()));

                String description = manager.getPropertyValue(AppPropertyTypes.DATA_FILE_EXT_DESC.name());
                String extension   = manager.getPropertyValue(AppPropertyTypes.DATA_FILE_EXT.name());
                ExtensionFilter extFilter = new ExtensionFilter(String.format("%s (.*%s)", description, extension),
                                                                String.format("*.%s", extension));

                fileChooser.getExtensionFilters().add(extFilter);
                File selected = fileChooser.showSaveDialog(applicationTemplate.getUIComponent().getPrimaryWindow());
                if (selected != null) {
                    dataFilePath = selected.toPath();
                    save();
                    ((AppUI) (applicationTemplate.getUIComponent())).getSaveButton().setDisable(true);
                } else return false; // if user presses escape after initially selecting 'yes'
            } else {
                save();
                ((AppUI) (applicationTemplate.getUIComponent())).getSaveButton().setDisable(true);
            }
        }

        return !dialog.getSelectedOption().equals(ConfirmationDialog.Option.CANCEL);
    }

    private void save() throws IOException {
        applicationTemplate.getDataComponent().saveData(dataFilePath);
        isUnsaved.set(false);

    }

    private void errorHandlingHelper() {
        ErrorDialog     dialog   = (ErrorDialog) applicationTemplate.getDialog(Dialog.DialogType.ERROR);
        PropertyManager manager  = applicationTemplate.manager;
        String          errTitle = manager.getPropertyValue(PropertyTypes.SAVE_ERROR_TITLE.name());
        String          errMsg   = manager.getPropertyValue(PropertyTypes.SAVE_ERROR_MSG.name());
        String          errInput = manager.getPropertyValue(AppPropertyTypes.SPECIFIED_FILE.name());
        dialog.show(errTitle, errMsg + errInput);
    }

    private boolean isSaved() {
        return dataFilePath != null;
    }

    public Path getDataFilePath() { return dataFilePath; }
}
