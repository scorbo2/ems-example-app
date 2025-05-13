package ca.corbett.ems.app.ui;

import ca.corbett.ems.app.handlers.UptimeHandler;
import ca.corbett.ems.client.EMSServerResponse;
import ca.corbett.ems.client.channel.Subscriber;
import ca.corbett.ems.client.channel.SubscriberEvent;
import ca.corbett.ems.client.channel.SubscriberListener;
import ca.corbett.ems.server.ChannelManager;
import ca.corbett.ems.server.EMSServer;
import ca.corbett.ems.server.EMSServerSpy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages connections to local or remote EMS servers and provides
 * convenience methods for executing commands against those servers.
 * Behind the scenes, we wrap both an EMSServer instance and also
 * a Subscriber (our derived EMSClient) instance. We can then
 * provide convenience methods to make it easy to start up a local
 * EMS server and connect to it, or to connect to a remote EMS server.
 * <p>
 *     <b>Connecting to a local server:</b>
 * </p>
 *     <pre>
 *         connectionManager.startLocalServer("localhost", 1975);
 *         connectionManager.connect("localhost", 1975);
 *     </pre>
 * <p>
 *     <b>Connecting to a remote server:</b>
 * </p>
 *     <pre>
 *         connectionManager.connect("10.0.0.75", 1975);
 *     </pre>
 *
 * <p>
 *     Either way, once you are connected, you can invoke any of the server-based
 *     methods, like retrieveChannelList or sendMessage, and
 *     ConnectionManager will route the commands to the local EMS server or to the
 *     remote one without the client having to worry about it.
 * </p>
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

    /**
     * ConnectionManager is singleton as the example app only displays a single connection
     * at a time. You can of course run multiple instances of the example app side-by-each
     * in order to test sending and receiving messages on one local EMS server.
     *
     * @return The single instance of ConnectionManager.
     */
    public static ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    /**
     * Registers a listener which will receive notifications as things happen
     * within ConnectionManager. See ConnectionListener for the description of
     * the various events that can be triggered.
     *
     * @param listener A ConnectionListener instance.
     */
    public void addConnectionListener(ConnectionListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given listener so that it no longer receives notifications from us.
     *
     * @param listener The listener to remove.
     */
    public void removeConnectionListener(ConnectionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Starts a local EMS server using the given host/IP and listening port. If any
     * local server was already running, it will be stopped. If the given parameters
     * were invalid (bad hostname, port already in use, etc), a connectionErrorEvent
     * will be triggered so that listeners know what happened. Attempting to connect
     * to the server after that point will fail.
     * <p>
     * If the server starts successfully, a localServerStartedEvent will be fired.
     * </p>
     *
     * @param host The hostname or IP address to use for the local server (usually "localhost")
     * @param port The port to listen on.
     */
    public boolean startLocalServer(String host, int port) {
        stopLocalServer();
        localServer = new EMSServer(host, port);
        //localServer.registerCommandHandler(new HaltHandler()); // nah
        localServer.registerCommandHandler(new UptimeHandler());
        localServer.startServer(); // we could spy on it for extra logging, but it'll get noisy
        localServer.addServerSpy(new UnsubscribeSpy());
        try {
            Thread.sleep(100); // give it a chance to start up
        } catch (InterruptedException ignored) {
        }
        if (!localServer.isUp()) {
            fireConnectionErrorEvent("Local server failed to start! Check your parameters.");
            return false;
        } else {
            fireLocalServerStartedEvent(host, port);
            return true;
        }
    }

    /**
     * Shuts down the local server if one was running. A localServerStoppedEvent
     * will be triggered in that case.
     */
    public void stopLocalServer() {
        if (isLocalServerRunning()) {
            localServer.stopServer();
            fireLocalServerStoppedEvent();
        }

        localServer = null;
    }

    /**
     * Attempts to subscribe to the given channels. This method is ignored if
     * we are not currently connected to an EMS server. A connectionErrorEvent
     * is triggered if the subscription fails. Otherwise, a
     * channelSubscribedEvent is triggered.
     * <p>
     * It's not an error condition to subscribe to a channel that
     * you are already subscribed to. Nothing will happen in that case.
     * </p>
     * <p>
     * It's also not an error to subscribe to a channel that does not yet
     * exist. The channel will be silently created.
     * </p>
     *
     * @param channelNames A list of names of channels to subscribe to.
     */
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

    /**
     * Unsubscribes from the named channels. A channelUnsubscribed event will
     * be triggered if the unsubscribe succeeds. If something goes wrong,
     * a connectionError event will be triggered.
     * <p>
     * It is not an error condition to unsubscribe from a channel
     * that you are not subscribed to. The server simply returns ok.
     * </p>
     * <p>
     * It is also not an error to unsubscribe from a channel that
     * does not exist. The server simply returns ok.
     * </p>
     * <p>
     * It is not an error to attempt to unsubscribe from the special
     * channel "ALL", but the request will not be honored and the
     * server will simply return ok. You can't unsubscribe from ALL.
     * </p>
     *
     * @param channelNames An array of channel names
     */
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

    /**
     * Attempts to connect to the given EMS server by hostname/IP and port.
     * The code at this point doesn't care if the given server is local
     * or remote.
     * <p>
     *     If the connection fails, a connectionError event will be triggered.
     *     This can happen if the host/port is unreachable. It can also
     *     happen if the EMS server responds but fails to return a list
     *     of channel names to us. (For example, you are connecting
     *     to an EMS server that does not support our expected commands).
     * </p>
     * <p>
     *     If the connection succeeds, a connected event is triggered.
     * </p>
     *
     * @param host The hostname or IP address of the EMS server.
     * @param port The listening port of the EMS server.
     */
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

    /**
     * Disconnects from the EMS server if we were connected, and also shuts down the
     * local EMS server if we were running one. A disconnected event is triggered
     * if we were connected, and a localServerStopped event is triggered if
     * there was a local server running.
     */
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

    /**
     * Reports whether we are currently connected to an EMS server.
     *
     * @return true if connected.
     */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    /**
     * Reports whether we are currently running a local EMS server.
     *
     * @return true if local EMS server is running.
     */
    public boolean isLocalServerRunning() {
        return localServer != null && localServer.isUp();
    }

    /**
     * Requests that the list of active channels, and also the list of channels
     * that we are currently subscribed to, should be refreshed. This method returns
     * a simple true or false if we were able to retrieve this information, but
     * the actual channel lists will be sent with the channelList event that
     * we will trigger. If an error occurs, a connectionError event is triggered.
     *
     * @return true if the server returned the channel lists to us.
     */
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

    /**
     * Sends the given message to all of the named channels. Note that EMS clients
     * do not receive the message that they themselves send! If you're subscribed
     * to channel X and you send a message to channel X and you're wondering why
     * you didn't receive your own message, that's why.
     * <p>
     *     <b>Super broadcasting</b><br>
     *     An easier way of sending a message to all channels is to select the
     *     special channel "ALL". This will notify all connected clients in one
     *     command, as opposed to naming all channels individually - this method
     *     will issue one SEND command for each entry in the channelNames array,
     *     which might be considerably slow if you're trying to send to everyone.
     * </p>
     *
     * @param toSend The message to send.
     * @param channelNames An array of channel names that should receive the message.
     */
    public void sendMessage(String toSend, String... channelNames) {
        for (String channel : channelNames) {
            client.broadcast(channel, toSend);
        }
    }

    /**
     * Invoked internally to query the server's ABOUT handler. Servers can assign
     * themselves any unique name, which can be discovered via this method.
     *
     * @return The server name, as reported by its ABOUT handler.
     */
    private String getServerVersion() {
        if (!isConnected()) {
            return null;
        }
        EMSServerResponse response = client.sendCommand("version");
        if (response.isSuccess()) {
            return response.getMessage();
        } else {
            return null;
        }
    }

    /**
     * Returns the clientId that the EMS server assigned us when we connected.
     * This information is not terribly useful except for informational purposes.
     *
     * @return The unique client id assigned to us by the EMS server.
     */
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

    private static class UnsubscribeSpy implements EMSServerSpy {

        @Override
        public void messageReceived(EMSServer server, String clientId, String rawMessage) {
        }

        @Override
        public void messageSent(EMSServer server, String clientId, String rawMessage) {
        }

        @Override
        public void clientConnected(EMSServer server, String clientId) {
        }

        @Override
        public void clientDisconnected(EMSServer server, String clientId) {
            ChannelManager.getInstance().unsubscribeFromAll(clientId);
        }
    }
}
