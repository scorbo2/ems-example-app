package ca.corbett.ems.app.handlers;

import ca.corbett.ems.app.ChannelManager;
import ca.corbett.ems.handlers.AbstractCommandHandler;
import ca.corbett.ems.server.EMSServer;

import java.util.List;

/**
 * Lists all channels on this EMS server that this client is
 * subscribed to. We don't include the special channel "ALL" because
 * this is the one channel that isn't optional.
 *
 * @author scorbo2
 * @since 2025-03-18
 */
public class ListSubscribedHandler extends AbstractCommandHandler {

    public ListSubscribedHandler() {
        super("LIST_SUBSCRIBED");
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
        return "Lists all channels you have subscribed to.";
    }

    @Override
    public String handle(EMSServer server, String clientId, String commandLine) {
        List<String> channels = ChannelManager.getInstance().getSubscribedChannels(clientId);
        if (channels.isEmpty()) {
            return "No subscriptions found.\n" + createOkResponse();
        }

        StringBuilder sb = new StringBuilder();
        for (String channel : channels) {
            sb.append(channel);
            sb.append("\n");
        }

        return sb.toString() + "\n" + createOkResponse();
    }
}
