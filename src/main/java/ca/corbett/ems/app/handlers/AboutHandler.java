package ca.corbett.ems.app.handlers;

import ca.corbett.ems.app.Version;
import ca.corbett.ems.handlers.AbstractCommandHandler;
import ca.corbett.ems.server.EMSServer;

/**
 * A simple command handler to return server version information.
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public class AboutHandler extends AbstractCommandHandler {

    private static AboutHandler instance;
    private static String serverName = Version.FULL_NAME;

    private AboutHandler() {
        super("about");
    }

    /**
     * This class is singleton to allow dynamically changing the server
     * name - see setServerName.
     *
     * @return The single instance of this command handler.
     */
    public static AboutHandler getInstance() {
        if (instance == null) {
            instance = new AboutHandler();
        }
        return instance;
    }

    /**
     * Set an optional name for this server. This will be returned when clients
     * query this ABOUT command. The default is the full name and version
     * of this application as specified in the Version class.
     *
     * @param name The new human-readable name for this server.
     */
    public void setServerName(String name) {
        serverName = (name == null || name.isBlank()) ? Version.FULL_NAME : name;
    }

    @Override
    public String handle(EMSServer server, String clientId, String commandLine) {
        return createOkResponse(serverName);
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
    public String getHelpText() {
        return "Displays application version information.";
    }

    @Override
    public String getUsageText() {
        return name;
    }

}
