package vilij.components;

import java.io.IOException;

/**
 * @author Ritwik Banerjee
 */
public interface ActionComponent {

    void handleNewRequest();

    void handleSaveRequest() throws IOException;

    void handleLoadRequest() throws IOException;

    void handleExitRequest() throws IOException;

    void handlePrintRequest() throws IOException;

}
