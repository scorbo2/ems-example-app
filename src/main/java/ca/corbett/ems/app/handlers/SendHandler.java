package ca.corbett.ems.app.handlers;

import ca.corbett.ems.app.ChannelManager;
import ca.corbett.ems.handlers.AbstractCommandHandler;
import ca.corbett.ems.server.EMSServer;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ca.corbett.ems.server.EMSServer.DELIMITER;

/**
 * Sends a message to a named channel. All subscribers to that channel will receive the message,
 * except for the client that sends it. It is not an error condition if the named channel does
 * not exist or has no subscribers - in any case, the command will return an ok response.
 * <p>
 *     <b>Super broadcasting</b><br>
 *     There's a special channel named "ALL" that all clients are automatically subscribed
 *     to and from which they can't unsubscribe. Sending a message to this channel
 *     will therefore notify all clients.
 * </p>
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public class SendHandler extends AbstractCommandHandler {

    private static final Logger logger = Logger.getLogger(SendHandler.class.getName());

    public SendHandler() {
        super("SEND");
    }

    @Override
    public int getMinParameterCount() {
        return 2;
    }

    @Override
    public int getMaxParameterCount() {
        return 2;
    }

    @Override
    public String getHelpText() {
        return "Sends a message to the specified channel.";
    }

    @Override
    public String handle(EMSServer server, String clientId, String commandLine) {
        String[] parts = getParts(commandLine);
        if (parts.length != 3) {
            return createErrorResponse("Expected 2 parameters (channel name and message)");
        }
        String channel = ChannelManager.sanitizeChannelName(parts[1]);
        List<String> clientsToNotify = ChannelManager.getInstance().getSubscribers(channel);
        if (clientsToNotify.isEmpty()) {
            logger.log(Level.FINE, "SendHandler: there are no listeners on channel \"{0}\"", channel);
        }

        for (String client : clientsToNotify) {
            if (!client.equals(clientId)) { // don't send it to the client that executed this command
                logger.log(Level.FINE, "SendHandler: sending \"{0}\" to {1} on channel {2}", new Object[]{parts[2], client, channel});
                server.sendToClient(client, channel + EMSServer.DELIMITER + parts[2]);
            }
        }
        return createOkResponse();
    }

    @Override
    public String getUsageText() {
        return name + DELIMITER + "<channel>" + DELIMITER + "<message>";
    }
}
