package ca.corbett.ems.app.handlers;

import ca.corbett.ems.app.ChannelManager;
import ca.corbett.ems.handlers.AbstractCommandHandler;
import ca.corbett.ems.server.EMSServer;

import static ca.corbett.ems.server.EMSServer.DELIMITER;

/**
 * @author scorbo2
 */
public class UnsubscribeHandler extends AbstractCommandHandler {

    public UnsubscribeHandler() {
        super("UNSUB", "UNSUBSCRIBE");
    }

    @Override
    public int getMinParameterCount() {
        return 1;
    }

    @Override
    public int getMaxParameterCount() {
        return 1;
    }

    @Override
    public String getHelpText() {
        return "Stops listening for messages on the given channel.";
    }

    @Override
    public String handle(EMSServer server, String clientId, String commandLine) {
        String[] parts = getParts(commandLine);
        if (parts.length != 2) {
            return createErrorResponse("Expected 1 parameter (channel name)");
        }
        ChannelManager.getInstance().unsubscribeFromChannel(clientId, parts[1]);
        return createOkResponse();
    }

    @Override
    public String getUsageText() {
        return name + DELIMITER + "<channel>";
    }

}
