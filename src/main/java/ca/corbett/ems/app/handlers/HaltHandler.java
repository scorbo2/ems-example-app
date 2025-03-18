package ca.corbett.ems.app.handlers;

import ca.corbett.ems.handlers.AbstractCommandHandler;
import ca.corbett.ems.server.EMSServer;

/**
 * @author scorbo2
 * @since 2023-11-24
 */
public class HaltHandler extends AbstractCommandHandler {

    private EMSServer server;

    public HaltHandler() {
        super("halt");
    }

    public void setEMSServer(EMSServer server) {
        this.server = server;
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
            } catch (InterruptedException ie) {
            }
            return EMSServer.DISCONNECTED;
        } else {
            return createErrorResponse("Server undefined.");
        }
    }
}
