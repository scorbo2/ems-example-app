package ca.corbett.ems.app;

import ca.corbett.ems.server.EMSServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A singleton class which manages all currently active channels on an EMS server.
 * A channel is considered active if there is at least one client subscribed to it.
 * Channel names are case-insensitive.
 * <p>
 *     A special channel called "ALL" exists by default, and all clients
 *     are automatically subscribed to it. You cannot unsubscribe from
 *     ALL and this channel can never be deleted. It exists so that there
 *     is always a way to reach other clients on an EMS server (for example,
 *     to notify them of server going down or whatever).
 * </p>
 * <p>
 *     Clients can use the SUBSCRIBE and UNSUBSCRIBE commands to start or stop
 *     listening to a specific channel. If SUBSCRIBE names a channel that is
 *     not currently active (or which has never existed), the named channel
 *     is created automatically. When all clients have unsubscribed from a given
 *     channel, it becomes inactive automatically.
 * </p>
 * <p>
 *     Clients can use the LIST_ACTIVE command to get a list of all currently
 *     active channels on the server. The LIST_SUBSCRIBED command is used to
 *     return a list of all channels to which that client is currently subscribed.
 * </p>
 * <p>
 *     The SEND command can send a message to a specific channel. If the
 *     target is the ALL channel, then all clients will receive that message
 *     regardless of their subscription status. Otherwise, only clients that
 *     are subscribed to the named channel will receive that message.
 * </p>
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public class ChannelManager {

    private static final Logger logger = Logger.getLogger(ChannelManager.class.getName());

    public static final String ALL_CHANNELS = "ALL";

    private static ChannelManager instance;

    /**
     * A Map of channel id to a Set of client ids. This represents
     * all active channels and lists all the clients that are
     * subscribed to each channel.
     */
    private final Map<String, Set<String>> channelSubscriberMap;

    private ChannelManager() {
        channelSubscriberMap = new HashMap<>();
    }

    /**
     * Subscribes the given client to the given channel. If the named channel does
     * not exist, it is created silently.
     *
     * @param clientId  The client id of the subscriber.
     * @param channelId The channel name (case-insensitive).
     */
    public void subscribeToChannel(String clientId, String channelId) {
        if (clientId == null || channelId == null || clientId.trim().isEmpty() || channelId.trim().isEmpty()) {
            logger.warning("subscribeToChannel with null or empty input - ignored.");
            return;
        }
        channelId = sanitizeChannelName(channelId);

        // You can't subscribe to "all" because you're already in.
        if (channelId.equalsIgnoreCase(ALL_CHANNELS)) {
            logger.warning("subscribeToChannel(ALL) ignored - this channel cannot be subscribed/unsubscribed.");
            return;
        }

        Set<String> subscriberList = channelSubscriberMap.get(channelId);
        if (subscriberList == null) {
            subscriberList = new HashSet<>();
            channelSubscriberMap.put(channelId, subscriberList);
            logger.info("Channel " + channelId + " is now active.");
        }

        logger.info("Client " + clientId + " is now subscribed to channel " + channelId);
        subscriberList.add(clientId);
    }

    /**
     * Unsubscribes the given client from the given channel, if such a subscription existed.
     *
     * @param clientId  The id of the client to be unsubscribed.
     * @param channelId The name of the channel in question.
     */
    public void unsubscribeFromChannel(String clientId, String channelId) {
        if (clientId == null || channelId == null || clientId.trim().isEmpty() || channelId.trim().isEmpty()) {
            logger.warning("unsubscribeFromChannel with null or empty input - ignored.");
            return;
        }

        channelId = sanitizeChannelName(channelId);

        // You can't unsubscribe to "all" because it's ununsubscribeable.
        if (channelId.equalsIgnoreCase(ALL_CHANNELS)) {
            logger.warning("unsubscribeFromChannel(ALL) ignored - this channel cannot be unsubscribed.");
            return;
        }
        Set<String> subscriberList = channelSubscriberMap.get(channelId);
        if (subscriberList == null) {
            logger.warning("unsubscribeFromChannel: client " + clientId + " is not subscribed to this channel. Ignoring.");
            return;
        }

        logger.info("Client " + clientId + " is now unsubscribed from channel " + channelId);
        subscriberList.remove(clientId);

        // If that was the last one, nuke this channel:
        if (subscriberList.isEmpty()) {
            logger.info("Channel " + channelId + " is no longer active (0 subscribers).");
            channelSubscriberMap.remove(channelId);
        }
    }

    /**
     * Returns a List of all active channels - that is, channels that have at least one
     * subscriber.
     *
     * @return A List of channel names. May be empty if the server has no activity.
     */
    public List<String> getActiveChannels() {
        List<String> channels = new ArrayList<>(channelSubscriberMap.keySet());
        channels.sort(null);
        return channels;
    }

    /**
     * Returns the list of channels that the given client is currently subscribed to.
     * We do not include the special channel ALL in this list as it is implied.
     *
     * @param clientId The client to search for.
     * @return A List of channels to which the client is subscribed (may be empty).
     */
    public List<String> getSubscribedChannels(String clientId) {
        List<String> subscriptions = new ArrayList<>();
        for (String channelId : channelSubscriberMap.keySet()) {
            if (channelSubscriberMap.get(channelId).contains(clientId)) {
                subscriptions.add(channelId);
            }
        }
        subscriptions.sort(null);
        return subscriptions;
    }

    /**
     * Returns a list of all clients that are currently subscribed to the given channel.
     * If the given channel is the special channel "ALL" then this will return a list of
     * all clients, because all clients are always subscribed to the ALL channel.
     *
     * @param channelId The channel in question (case-insensitive).
     * @return A List of zero or more client ids subscribed to the channel in question.
     */
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
            list.sort(null);
            return list;
        }

        // Otherwise you just get the subscribers of the channel you mentioned:
        Set<String> subscriberList = channelSubscriberMap.get(channelId);
        if (subscriberList != null) {
            list.addAll(subscriberList);
            list.sort(null);
        }

        return list;
    }

    /**
     * Returns the single static instance of this class.
     *
     * @return The ChannelManager.
     */
    public static ChannelManager getInstance() {
        if (instance == null) {
            instance = new ChannelManager();
        }
        return instance;
    }

    /**
     * Trims leading and trailing whitespace, converts to upper case, and removes
     * any EMS delimiters that are present in the given channel name.
     *
     * @param channel The name to be sanitized.
     * @return The sanitized name (note the return may be blank if the input was garbage!)
     */
    public static String sanitizeChannelName(String channel) {
        String sanitized = channel.trim().toUpperCase();

        // Stupid special case: strip out delimiters if there are any:
        return sanitized.replaceAll(EMSServer.DELIMITER, "");
    }
}
