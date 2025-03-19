package ca.corbett.ems.app.handlers;

import ca.corbett.ems.app.ChannelManager;
import ca.corbett.ems.handlers.AbstractCommandHandler;
import ca.corbett.ems.server.EMSServer;

import java.util.List;

/**
 * Lists all active (that is, at least one subscriber) channels on the server.
 * Channels with no active subscribers are not returned. We don't include
 * the special channel "ALL" as that one is implied.
 *
 * @author scorbo2
 * @since 2025-03-18
 */
public class ListActiveHandler extends AbstractCommandHandler {

    public ListActiveHandler() {
        super("LIST_ACTIVE");
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
        return "Lists all active channels on this EMS server.";
    }

    @Override
    public String handle(EMSServer server, String clientId, String commandLine) {
        List<String> channels = ChannelManager.getInstance().getActiveChannels();
        if (channels.isEmpty()) {
            return createOkResponse();
        }

        StringBuilder sb = new StringBuilder();
        for (String channel : channels) {
            sb.append(channel);
            sb.append("\n");
        }

        return sb.toString() + "\n" + createOkResponse();
    }
}
