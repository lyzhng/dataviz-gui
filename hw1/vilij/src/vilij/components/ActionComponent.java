package vilij.components;

/**
 * @author Ritwik Banerjee
 */
public interface ActionComponent {

    void handleNewRequest();

    void handleSaveRequest();

    void handleLoadRequest();

    void handleExitRequest();

    void handlePrintRequest();

}
