package ca.corbett.ems.app.handlers;

import ca.corbett.ems.handlers.AbstractCommandHandler;
import ca.corbett.ems.server.EMSServer;

/**
 * Disconnects all clients and shuts down the server.
 * This one probably shouldn't be available to all clients as it's
 * a bit powerful, but eh. Security is not an EMS consideration.
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public class HaltHandler extends AbstractCommandHandler {

    public HaltHandler() {
        super("halt");
    }

    @Override
    public int getMinParameterCount() {
        return 0;
    }

    @Override
    public int getMaxParameterCount() {
        return 0;
    }

    @Override
    public String getUsageText() {
        return name;
    }

    @Override
    public String getHelpText() {
        return "Shuts down the server.";
    }

    @Override
    public String handle(EMSServer server, String clientId, String commandLine) {
        if (server != null) {
            server.stopServer();
            try {
                Thread.sleep(100); // give it a chance to kill client connections
            } catch (InterruptedException ignored) {
            }
            return EMSServer.DISCONNECTED;
        } else {
            return createErrorResponse("Server undefined.");
        }
    }
}
