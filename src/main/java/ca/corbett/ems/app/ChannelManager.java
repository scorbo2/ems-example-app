package ca.corbett.ems.app;

import ca.corbett.ems.server.EMSServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author scorbo2
 * @since 2023-11-24
 */
public class ChannelManager {

    public static final String ALL_CHANNELS = "ALL";

    private static ChannelManager instance;
    private final Map<String, Set<String>> channelSubscriberMap;

    private ChannelManager() {
        channelSubscriberMap = new HashMap<>();
    }

    public void subscribeToChannel(String clientId, String channelId) {
        if (clientId == null || channelId == null || clientId.trim().isEmpty() || channelId.trim().isEmpty()) {
            return;
        }
        channelId = sanitizeChannelName(channelId);

        // You can't subscribe to "all" because you're already in.
        if (channelId.equalsIgnoreCase(ALL_CHANNELS)) {
            return;
        }

        Set<String> subscriberList = channelSubscriberMap.get(channelId);
        if (subscriberList == null) {
            subscriberList = new HashSet<>();
            channelSubscriberMap.put(channelId, subscriberList);
        }

        subscriberList.add(clientId);
    }

    public void unsubscribeFromChannel(String clientId, String channelId) {
        if (clientId == null || channelId == null || clientId.trim().isEmpty() || channelId.trim().isEmpty()) {
            return;
        }

        channelId = sanitizeChannelName(channelId);

        // You can't unsubscribe to "all" because it's ununsubscribeable.
        if (channelId.equalsIgnoreCase(ALL_CHANNELS)) {
            return;
        }
        Set<String> subscriberList = channelSubscriberMap.get(channelId);
        if (subscriberList == null) {
            return;
        }

        subscriberList.remove(clientId);
    }

    public List<String> getSubscribers(String channelId) {
        List<String> list = new ArrayList<>();
        if (channelId == null || channelId.trim().isEmpty()) {
            return list;
        }

        channelId = sanitizeChannelName(channelId);

        // Special case: if you ask for "all" you're getting all:
        if (channelId.equalsIgnoreCase(ALL_CHANNELS)) {
            Set<String> allClients = new HashSet<>();
            for (String key : channelSubscriberMap.keySet()) {
                allClients.addAll(channelSubscriberMap.get(key));
            }
            list.addAll(allClients);
            return list;
        }

        // Otherwise you just get the subscribers of the channel you mentioned:
        Set<String> subscriberList = channelSubscriberMap.get(channelId);
        if (subscriberList != null) {
            list.addAll(subscriberList);
        }

        return list;
    }

    public static ChannelManager getInstance() {
        if (instance == null) {
            instance = new ChannelManager();
        }
        return instance;
    }

    public static String sanitizeChannelName(String channel) {
        String sanitized = channel.trim(); // We could toUpperCase() here to make channels case insensitive...

        // Stupid special case: strip out delimiters if there are any:
        return sanitized.replaceAll(EMSServer.DELIMITER, "");
    }

}
