package actions;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ui.AppUI;
import ui.DataVisualizer;
import vilij.components.ActionComponent;
import vilij.components.ConfirmationDialog;
import vilij.components.Dialog;
import vilij.templates.ApplicationTemplate;
import vilij.templates.UITemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static settings.AppPropertyTypes.*;

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
    FileWriter fileWriter;

    public AppActions(ApplicationTemplate applicationTemplate) {
        this.applicationTemplate = applicationTemplate;
    }

    @Override
    public void handleNewRequest() {
        // TODO for homework 1
        // Confirmation dialog appears, asking Y/N/Cancel.
        // TODO: Hard-coded...
        try {
            applicationTemplate.getDialog(Dialog.DialogType.CONFIRMATION)
                    .show( applicationTemplate.manager.getPropertyValue(SAVE_UNSAVED_WORK_TITLE.name()), applicationTemplate.manager.getPropertyValue(SAVE_UNSAVED_WORK.name()));

            // TODO: When "YES" is clicked, prompt to save the file.

            ConfirmationDialog.Option option =
                    ((ConfirmationDialog) (applicationTemplate.getDialog(Dialog.DialogType.CONFIRMATION)))
                            .getSelectedOption();

            if (option == ConfirmationDialog.Option.YES) {
                if (promptToSave()) {
                    String dir = dataFilePath.getParent().toString();
                    String fileName = dataFilePath.getFileName().toString();
                    File file = new File(dir, fileName);
                    fileWriter = new FileWriter(file);

                    String text = ((AppUI) (applicationTemplate.getUIComponent())).getText();
                    for (String s : text.split("\n"))
                        fileWriter.write(s + "\n");

                    fileWriter.flush();
                }
                // TODO: On keypress ESC, the user will return back to the main interface.
                // On keypress ESC, the window closes by itself. Nothing to implement.
            }

            if (option == ConfirmationDialog.Option.NO) {
                // TODO: Confirmation log will disappear. Text area is empty. Disabled New/Save.
                // dialog.close()
                applicationTemplate.getUIComponent().clear();
            }

            if (option == ConfirmationDialog.Option.CANCEL) {
                // By default, it works that way.
            }
        }
        catch (IOException e) {
            applicationTemplate.
                    getDialog(Dialog.DialogType.ERROR)
                    .show( applicationTemplate.manager.getPropertyValue(RESOURCE_SUBDIR_NOT_FOUND.name()) , e.getMessage());
        }

    }

    @Override
    public void handleSaveRequest() {
        // TODO: NOT A PART OF HW 1
    }

    @Override
    public void handleLoadRequest() {
        // TODO: NOT A PART OF HW 1
    }

    @Override
    public void handleExitRequest() {
        // TODO for homework 1
        Platform.exit();
        System.exit(0);
    }

    @Override
    public void handlePrintRequest() {
        // TODO: NOT A PART OF HW 1
    }

    public void handleScreenshotRequest() throws IOException {
        // TODO: NOT A PART OF HW 1
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
        // TODO for homework 1
        // TODO remove the placeholder line below after you have implemented this method
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter( applicationTemplate.manager.getPropertyValue(DATA_FILE_EXT_DESC.name()), DATA_FILE_EXT.name())
        );
        File selectedFile = fileChooser.showSaveDialog( applicationTemplate.getUIComponent().getPrimaryWindow() );

        if (selectedFile != null) {
            dataFilePath = selectedFile.toPath();
            return true;
        }

        else
            return false;
    }
}
