package ca.corbett.ems.app.ui;

import ca.corbett.ems.app.handlers.AboutHandler;
import ca.corbett.ems.app.handlers.HelpHandler;
import ca.corbett.ems.app.handlers.ListActiveHandler;
import ca.corbett.ems.app.handlers.ListSubscribedHandler;
import ca.corbett.ems.app.handlers.SendHandler;
import ca.corbett.ems.app.handlers.SubscribeHandler;
import ca.corbett.ems.app.handlers.UnsubscribeHandler;
import ca.corbett.ems.app.handlers.UptimeHandler;
import ca.corbett.ems.app.subscriber.Subscriber;
import ca.corbett.ems.app.subscriber.SubscriberEvent;
import ca.corbett.ems.app.subscriber.SubscriberListener;
import ca.corbett.ems.client.EMSServerResponse;
import ca.corbett.ems.server.EMSServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages connections to local or remote EMS servers and provides
 * convenience methods for executing commands against those servers.
 *
 * @author scorbo2
 * @since 2025-03-19
 */
public final class ConnectionManager {

    private static final Logger logger = Logger.getLogger(ConnectionManager.class.getName());

    private static ConnectionManager instance;

    private final List<ConnectionListener> listeners = new ArrayList<>();
    private EMSServer localServer;
    private Subscriber client;

    private ConnectionManager() {

    }

    public static ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    public void addConnectionListener(ConnectionListener listener) {
        listeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        listeners.remove(listener);
    }

    public void startLocalServer(String host, int port) {
        stopLocalServer();
        localServer = new EMSServer(host, port);
        localServer.registerCommandHandler(AboutHandler.getInstance());
        localServer.registerCommandHandler(new HelpHandler());
        localServer.registerCommandHandler(new SendHandler());
        localServer.registerCommandHandler(new ListActiveHandler());
        localServer.registerCommandHandler(new ListSubscribedHandler());
        localServer.registerCommandHandler(new SubscribeHandler());
        localServer.registerCommandHandler(new UnsubscribeHandler());
        //localServer.registerCommandHandler(new HaltHandler()); // nah
        localServer.registerCommandHandler(new UptimeHandler());
        localServer.startServer(); // we could spy on it for extra logging, but it'll get noisy
        fireLocalServerStartedEvent(host, port);
    }

    public void stopLocalServer() {
        if (isLocalServerRunning()) {
            localServer.stopServer();
            fireLocalServerStoppedEvent();
        }

        localServer = null;
    }

    public void subscribe(String... channelNames) {
        if (!isConnected() || channelNames == null || channelNames.length == 0) {
            return;
        }

        boolean success = true;
        for (String channel : channelNames) {
            success = success && client.subscribe(channel);
            if (success) {
                fireChannelSubscribedEvent(channel);
            }
        }
        if (!success) {
            fireConnectionErrorEvent("Channel subscription failed!");
        }
        retrieveChannelList();
    }

    public void unsubscribe(String... channelNames) {
        if (!isConnected() || channelNames == null || channelNames.length == 0) {
            return;
        }

        boolean success = true;
        for (String channel : channelNames) {
            success = success && client.unsubscribe(channel);
            if (success) {
                fireChannelUnsubscribedEvent(channel);
            }
        }
        if (!success) {
            fireConnectionErrorEvent("Channel unsubscription failed!");
        }
        retrieveChannelList();
    }

    public void connect(String host, int port) {
        if (isConnected()) {
            client.disconnect();
            fireDisconnectedEvent();
        }
        client = new Subscriber();
        client.addSubscriberEventListener(new SubscriberListener() {
            @Override
            public void connected(SubscriberEvent event) {
                // ignored - we cover this manually
            }

            @Override
            public void disconnected(SubscriberEvent event) {
                disconnect();
            }

            @Override
            public void channelMessageReceived(SubscriberEvent event, String message) {
                fireChannelMessageReceivedEvent(event.getChannel(), message);
            }
        });
        if (client.connect(host, port)) {
            String serverVersion = getServerVersion();
            String clientId = getClientId();
            if (serverVersion == null || clientId == null) {
                fireConnectionErrorEvent("Unable to query server! Disconnecting...");
                disconnect();
                return;
            }
            if (!retrieveChannelList()) {
                // RetrieveChannelList will fire off a connection error event if it fails,
                // so here we can just disconnect and return.
                disconnect();
                return;
            }
            fireConnectedEvent(host, port, serverVersion, clientId);
        } else {
            fireConnectionErrorEvent("Unable to connect.");
        }
    }

    public void disconnect() {
        if (isConnected()) {
            client.disconnect();
            fireDisconnectedEvent();
        }
        if (isLocalServerRunning()) {
            stopLocalServer();
        }

        client = null;
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public boolean isLocalServerRunning() {
        return localServer != null && localServer.isUp();
    }

    public boolean retrieveChannelList() {
        List<String> activeChannels = retrieveChannelList("LIST_ACTIVE");
        List<String> subscribedChannels = retrieveChannelList("LIST_SUBSCRIBED");
        if (activeChannels != null && subscribedChannels != null) {
            fireChannelListEvent(activeChannels, subscribedChannels);
            return true;
        } else {
            fireConnectionErrorEvent("Failed to retrieve channel list!");
            return false;
        }
    }

    /**
     * Invoked internally to retrieve a list of channels from either
     * the LIST_ACTIVE command or the LIST_SUBSCRIBED command.
     * Disconnects and logs an error if anything goes wrong.
     *
     * @param command Either LIST_ACTIVE or LIST_SUBSCRIBED.
     * @return A List of channel names. Might be empty.
     */
    private List<String> retrieveChannelList(String command) {
        if (client == null || !client.isConnected()) {
            return null;
        }

        EMSServerResponse response = client.sendCommand(command);
        if (!client.isConnected() || response.isError()) {
            logger.log(Level.SEVERE, "Failed to gather channel list from server... aborting.");
            disconnect();
        } else {
            List<String> channels = new ArrayList<>();
            String responseMsg = response.getMessage().trim();
            if (responseMsg.isBlank()) {
                return channels;
            }
            Collections.addAll(channels, responseMsg.split("\n"));
            return channels;
        }

        return null;
    }

    public void sendMessage(String toSend, String... channelNames) {
        for (String channel : channelNames) {
            client.broadcast(channel, toSend);
        }
    }

    private String getServerVersion() {
        if (!isConnected()) {
            return null;
        }
        EMSServerResponse response = client.sendCommand("about");
        if (response.isSuccess()) {
            return response.getMessage();
        } else {
            return null;
        }
    }

    private String getClientId() {
        if (!isConnected()) {
            return null;
        }
        EMSServerResponse response = client.sendCommand("who");
        if (response.isSuccess()) {
            return response.getMessage();
        } else {
            return null;
        }
    }

    private void fireLocalServerStartedEvent(String host, int port) {
        for (ConnectionListener listener : listeners) {
            listener.localServerStarted(host, port);
        }
    }

    private void fireLocalServerStoppedEvent() {
        for (ConnectionListener listener : listeners) {
            listener.localServerStopped();
        }
    }

    private void fireConnectedEvent(String host, int port, String serverVersion, String clientId) {
        for (ConnectionListener listener : listeners) {
            listener.connected(host, port, serverVersion, clientId);
        }
    }

    private void fireDisconnectedEvent() {
        for (ConnectionListener listener : listeners) {
            listener.disconnected();
        }
    }

    private void fireConnectionErrorEvent(String errorMessage) {
        for (ConnectionListener listener : listeners) {
            listener.connectionError(errorMessage);
        }
    }

    private void fireChannelMessageReceivedEvent(String channel, String message) {
        for (ConnectionListener listener : listeners) {
            listener.channelMessageReceived(channel, message);
        }
    }

    private void fireChannelListEvent(List<String> activeChannels, List<String> subscribedChannels) {
        for (ConnectionListener listener : listeners) {
            listener.channelList(activeChannels, subscribedChannels);
        }
    }

    private void fireChannelSubscribedEvent(String channelName) {
        for (ConnectionListener listener : listeners) {
            listener.channelSubscribed(channelName);
        }
    }

    private void fireChannelUnsubscribedEvent(String channelName) {
        for (ConnectionListener listener : listeners) {
            listener.channelUnsubscribed(channelName);
        }
    }
}
