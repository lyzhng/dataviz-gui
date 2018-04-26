package actions;

import algorithms.Algorithm;
import algorithms.RandomClassifier;
import dataprocessors.AppData;
import dialogs.ExitWhileUnfinishedDialog;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static settings.AppPropertyTypes.*;
import static vilij.settings.PropertyTypes.*;
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
    static final String SEPARATOR = "/";
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
            Algorithm alg = ((AppData) applicationTemplate.getDataComponent()).getRandomClassifier();
            if (alg != null) {
                if (!alg.finishedRunning()) {
                    ErrorDialog errorDialog = ErrorDialog.getDialog();
                    String title = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.UNFINISHED_RUN.name());
                    String msg = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.UNFINISHED_NEWDATA_WARNING.name());
                    errorDialog.show(title, msg);
                }
            }
            if (!isUnsaved.get() || promptToSave()) {
                AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
                applicationTemplate.getDataComponent().clear();
                applicationTemplate.getUIComponent().clear();
                dataFilePath = null;
                uiComponent.enableTextArea();
                uiComponent.showTextArea();
                uiComponent.getTextArea().setEditable(true);
                uiComponent.showToggles();
            }
        } catch (IOException e) { errorHandlingHelper(); }
    }

    @Override
    public void handleSaveRequest() {
        Algorithm alg = ((AppData) applicationTemplate.getDataComponent()).getRandomClassifier();
        if (alg != null) {
            if (!alg.finishedRunning()) {
                ErrorDialog errorDialog = ErrorDialog.getDialog();
                String title = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.UNFINISHED_RUN.name());
                String msg = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.UNFINISHED_SAVE_WARNING.name());
                errorDialog.show(title, msg);
            }
        }
        try {
            AtomicBoolean hadAnError = ((AppData) applicationTemplate.getDataComponent()).hadAnError();
            applicationTemplate.getDataComponent().clear();
            TextArea textArea = ((AppUI) applicationTemplate.getUIComponent()).getTextArea();
            ((AppData) applicationTemplate.getDataComponent()).getProcessor().processString(textArea.getText());
            int duplicate = ((AppData) applicationTemplate.getDataComponent()).checkForDuplicates(textArea.getText());
            if (duplicate != -1) { // duplicate error
                List<String> lines = Arrays.asList(textArea.getText().split(System.lineSeparator()));
                String errLoadTitle = applicationTemplate.manager.getPropertyValue(LOAD_ERROR_TITLE.name());
                String errDupMsg = applicationTemplate.manager.getPropertyValue(DUPLICATE_ERR_MSG.name());
                String errDupCont = (duplicate+1) + System.lineSeparator() + lines.get(duplicate);
                ErrorDialog errorDialog = ErrorDialog.getDialog();
                errorDialog.show(errLoadTitle, errDupMsg + errDupCont);
                ((AppUI) applicationTemplate.getUIComponent()).getSaveButton().setDisable(true);
                hadAnError.set(true);
                return;
            }
            else if (isSaved() && !hadAnError.get()) {
                save();
            } else if (!isSaved() && !hadAnError.get()){
                promptToSave();
            } else {
                String errSaveTitle = applicationTemplate.manager.getPropertyValue(SAVE_ERROR_TITLE.name());
                String errSaveMsg = applicationTemplate.manager.getPropertyValue(SAVE_ERROR_MSG.name());
                String errSaveMsgCont = applicationTemplate.manager.getPropertyValue(DATA_FILE_LABEL.name()) +
                        "\n";
                ErrorDialog errorDialog = ErrorDialog.getDialog();
                errorDialog.show(errSaveTitle, errSaveMsg + errSaveMsgCont);
            }

            if (isSaved() || hadAnError.get()) {
                ((AppUI) (applicationTemplate.getUIComponent())).getSaveButton().setDisable(true);
            }

        } catch (IOException e) {
            String errSaveTitle = applicationTemplate.manager.getPropertyValue(SAVE_ERROR_TITLE.name());
            String errSaveMsg = applicationTemplate.manager.getPropertyValue(SAVE_ERROR_MSG.name());
            String errSaveMsgCont = applicationTemplate.manager.getPropertyValue(DATA_FILE_LABEL.name());
            ErrorDialog errorDialog = ErrorDialog.getDialog();
            errorDialog.show(errSaveTitle, errSaveMsg + errSaveMsgCont);
            ((AppData) applicationTemplate.getDataComponent()).hadAnError().set(true);
        } catch (Exception e) { // format error
            String errSaveTitle = applicationTemplate.manager.getPropertyValue(SAVE_ERROR_TITLE.name());
            String errSaveMsg = applicationTemplate.manager.getPropertyValue(SAVE_ERROR_MSG.name());
            String errLoadMsg =
                    applicationTemplate.manager.getPropertyValue(INCORRECT_FORMAT.name()) +
                            e.getMessage().substring(e.getMessage().indexOf("\n")+1);
            String errSaveMsgCont = applicationTemplate.manager.getPropertyValue(DATA_FILE_LABEL.name()) + "\n" +
                    errLoadMsg;
            ErrorDialog errorDialog = ErrorDialog.getDialog();
            errorDialog.show(errSaveTitle, errSaveMsg + errSaveMsgCont);
            ((AppData) applicationTemplate.getDataComponent()).hadAnError().set(true);
        }
    }

    @Override
    public void handleLoadRequest() {
        try {
            AppUI uiComponent = ((AppUI) applicationTemplate.getUIComponent());
            AppData dataComponent = (AppData) applicationTemplate.getDataComponent();

            Algorithm alg = ((AppData) applicationTemplate.getDataComponent()).getRandomClassifier();
            if (alg != null) {
                if (!alg.finishedRunning()) {
                    ErrorDialog errorDialog = ErrorDialog.getDialog();
                    String title = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.UNFINISHED_RUN.name());
                    String msg = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.UNFINISHED_LOAD_WARNING.name());
                    errorDialog.show(title, msg);
                    return;
                }
            }

            uiComponent.clearChart();
            dataComponent.clear();
            FileChooser fileChooser = new FileChooser();
            String tsdFullName = applicationTemplate.manager.getPropertyValue(TSD_FULL_NAME.name());
            String tsdExt = applicationTemplate.manager.getPropertyValue(TSD_EXT.name());
            FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(tsdFullName, tsdExt);
            fileChooser.getExtensionFilters().add(extensionFilter);
            String dataDirPath = SEPARATOR + applicationTemplate.manager.getPropertyValue(DATA_RESOURCE_PATH.name());
            URL dataDirURL = getClass().getResource(dataDirPath);
            fileChooser.setInitialDirectory(new File(dataDirURL.getFile()));
            File selected = fileChooser.showOpenDialog(applicationTemplate.getUIComponent().getPrimaryWindow());

            if (selected != null) {
                applicationTemplate.getDataComponent().loadData(selected.toPath());
                uiComponent.getSaveButton().setDisable(true);

                if (!dataComponent.hadAnError().get())
                    uiComponent.showStatsAndAlgorithm();
                else {
                    uiComponent.hideStats();
                    uiComponent.hideAlgorithmTypes();
                }

                uiComponent.showTextArea();
                uiComponent.hideToggles();
                uiComponent.showAlgorithmTypes();
                uiComponent.chosenListHandler();
                uiComponent.radioButtonHandler();
            }
        } catch (Exception e) {
            String errLoadTitle = applicationTemplate.manager.getPropertyValue(LOAD_ERROR_TITLE.name());
            String errLoadMsg = applicationTemplate.manager.getPropertyValue(LOAD_ERROR_MSG.name());
            String errLoadMsgCont = applicationTemplate.manager.getPropertyValue(DATA_FILE_LABEL.name());
            ErrorDialog errorDialog = ErrorDialog.getDialog();
            errorDialog.show(errLoadTitle, errLoadMsg + errLoadMsgCont);
        }
    }

    @Override
    public void handleExitRequest() {
        try {
            // add code for alg is running
            Algorithm alg = ((AppData) applicationTemplate.getDataComponent()).getRandomClassifier();
            ExitWhileUnfinishedDialog dialog = ExitWhileUnfinishedDialog.getDialog();
            if (alg != null) {
                // algorithm has been selected & is not finished running
                if (!alg.finishedRunning()) {
                    // exit anyway | cancel
                    String title = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.UNFINISHED_RUN.name());
                    String msg = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.EXIT_WHILE_RUNNING_WARNING.name());
                    dialog.show(title, msg);
                    ExitWhileUnfinishedDialog.Option selectedOption = dialog.getSelectedOption();
                    if (selectedOption.name().equalsIgnoreCase(ExitWhileUnfinishedDialog.Option.RETURN.name())) {
                        return;
                    }
                    if (selectedOption.name().equalsIgnoreCase(ExitWhileUnfinishedDialog.Option.EXIT.name())) {
                        System.exit(0);
                    }
                }
                // algorithm has finished running
                else {
                    if (isUnsaved.get()) { promptToSave(); }
                    else { save(); }
                }
                System.exit(0);
            }
            // algorithm has never been selected | start of app
            else {
                if (isUnsaved.get()) { promptToSave(); }
                else { save(); }
                System.exit(0);
            }
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
        String      dataDirPath = SEPARATOR + applicationTemplate.manager.getPropertyValue(DATA_RESOURCE_PATH.name());
        URL         dataDirURL  = getClass().getResource(dataDirPath);
        fileChooser.setInitialDirectory(new File(dataDirURL.getFile()));
        fileChooser.setTitle(applicationTemplate.manager.getPropertyValue(SAVE_WORK_TITLE.name()));
        File location = fileChooser.showSaveDialog(applicationTemplate.getUIComponent().getPrimaryWindow());

        try {
            String ext = applicationTemplate.manager.getPropertyValue(PNG_EXT.name());
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
        dialog.show(manager.getPropertyValue(SAVE_UNSAVED_WORK_TITLE.name()),
                    manager.getPropertyValue(SAVE_UNSAVED_WORK.name()));

        if (dialog.getSelectedOption() == null) return false; // if user closes dialog using the window's close button

        if (dialog.getSelectedOption().equals(ConfirmationDialog.Option.YES)) {

            if (dataFilePath == null) {
                FileChooser fileChooser = new FileChooser();
                String      dataDirPath = SEPARATOR + manager.getPropertyValue(DATA_RESOURCE_PATH.name());
                URL         dataDirURL  = getClass().getResource(dataDirPath);

                if (dataDirURL == null)
                    throw new FileNotFoundException(manager.getPropertyValue(RESOURCE_SUBDIR_NOT_FOUND.name()));

                fileChooser.setInitialDirectory(new File(dataDirURL.getFile()));
                fileChooser.setTitle(manager.getPropertyValue(SAVE_WORK_TITLE.name()));

                String description = manager.getPropertyValue(DATA_FILE_EXT_DESC.name());
                String extension   = manager.getPropertyValue(DATA_FILE_EXT_NAME.name());
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
        String          errTitle = manager.getPropertyValue(SAVE_ERROR_TITLE.name());
        String          errMsg   = manager.getPropertyValue(SAVE_ERROR_MSG.name());
        String          errInput = manager.getPropertyValue(SPECIFIED_FILE.name());
        dialog.show(errTitle, errMsg + errInput);
    }

    private boolean isSaved() {
        return dataFilePath != null;
    }
}
