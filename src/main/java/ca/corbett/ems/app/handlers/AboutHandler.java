package ca.corbett.ems.app.handlers;

import ca.corbett.ems.app.Version;
import ca.corbett.ems.handlers.AbstractCommandHandler;
import ca.corbett.ems.server.EMSServer;

/**
 * A simple command handler to return server information.
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public class AboutHandler extends AbstractCommandHandler {

    public AboutHandler() {
        super("about");
    }

    @Override
    public String handle(EMSServer server, String clientId, String commandLine) {
        return createOkResponse(Version.FULL_NAME);
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
