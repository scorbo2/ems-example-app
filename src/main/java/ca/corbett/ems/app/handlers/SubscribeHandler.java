package ca.corbett.ems.app.handlers;

import ca.corbett.ems.app.ChannelManager;
import ca.corbett.ems.handlers.AbstractCommandHandler;
import ca.corbett.ems.server.EMSServer;

import static ca.corbett.ems.server.EMSServer.DELIMITER;

/**
 * Subscribes to the named channel. From that point on, the client issuing this command
 * will receive notification when any message is sent to that channel. To stop receiving
 * messages for that channel, the client can use the unsubscribe command.
 * <p>
 *     It is not an error condition if the named channel does not exist. An okay
 *     response will be returned in any case.
 * </p>
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public class SubscribeHandler extends AbstractCommandHandler {

    public SubscribeHandler() {
        super("SUB", "SUBSCRIBE");
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
        return "Starts listening for messages on the given channel.";
    }

    @Override
    public String handle(EMSServer server, String clientId, String commandLine) {
        String[] parts = getParts(commandLine);
        if (parts.length != 2) {
            return createErrorResponse("Expected 1 parameter (channel name)");
        }
        ChannelManager.getInstance().subscribeToChannel(clientId, parts[1]);
        return createOkResponse();
    }

    @Override
    public String getUsageText() {
        return name + DELIMITER + "<channel>";
    }
}
