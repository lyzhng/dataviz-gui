package dialogs;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import vilij.components.ConfirmationDialog;
import vilij.components.Dialog;

import java.util.Arrays;
import java.util.List;

/**
 * @author The author of this document is Lily Zhong.
 */
public class ExitWhileUnfinishedDialog extends Stage implements Dialog
{
    // push change
    /**
     * This class provides the template for displaying three-way confirmation messages to the end user of a Vilij
     * application. It always provides the two controls: (i) a customizable message, and (ii) a set of three buttons
     * corresponding to the three standard options for the user. The three options (<code>Yes</code>, <code>No</code>, and
     * <code>Cancel</code> are modeled as the inner enumerable class {@link vilij.components.ConfirmationDialog.Option}.
     *
     * @author Ritwik Banerjee
     */
        public enum Option {

            RETURN("Return"),
            EXIT("Exit");

            @SuppressWarnings("unused")
            private String option;

            Option(String option) { this.option = option; }
        }

        private static ExitWhileUnfinishedDialog dialog;

        private Label message = new Label();
        private ExitWhileUnfinishedDialog.Option selectedOption;

        private ExitWhileUnfinishedDialog() { /* empty constructor */ }

        public static ExitWhileUnfinishedDialog getDialog() {
            if (dialog == null)
                dialog = new ExitWhileUnfinishedDialog();
            return dialog;
        }

        private void setMessage(String msg) { message.setText(msg); }

        private void deleteOptionHistory()                  { selectedOption = null; }

        /**
         * Completely initializes the error dialog to be used.
         *
         * @param owner the window on top of which the error dialog window will be displayed
         */
        @Override
        public void init(Stage owner) {
            initModality(Modality.WINDOW_MODAL); // modal => messages are blocked from reaching other windows
            initOwner(owner);

            List<Button> buttons = Arrays.asList(
                    new Button(Option.RETURN.name()),
                    new Button(Option.EXIT.name()));

            buttons.forEach(button -> button.setOnAction((ActionEvent event) -> {
                this.selectedOption = Option.valueOf(((Button) event.getSource()).getText());
                this.hide();
            }));

            HBox buttonBox = new HBox(5);
            buttonBox.getChildren().addAll(buttons);

            VBox messagePane = new VBox(message, buttonBox);
            messagePane.setAlignment(Pos.CENTER);
            messagePane.setPadding(new Insets(10, 20, 20, 20));
            messagePane.setSpacing(10);

            this.setScene(new Scene(messagePane));
        }

        /**
         * Loads the specified title and message into the error dialog and then displays the dialog.
         *
         * @param dialogTitle the specified dialog title
         * @param message     the specified message
         */
        @Override
        public void show(String dialogTitle, String message) {
            deleteOptionHistory();           // delete any previously selected option
            setTitle(dialogTitle);           // set the title of the dialog
            setMessage(message); // set the main error message
            showAndWait();                   // open the dialog and wait for the user to click the close button
        }


        public ExitWhileUnfinishedDialog.Option getSelectedOption() { return selectedOption; }
    }

