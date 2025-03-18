package ca.corbett.ems.app.subscriber;

import ca.corbett.ems.client.EMSClient;
import ca.corbett.ems.client.EMSServerResponse;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of EMSClient specifically for subscribing to channels on an
 * EMS Server that supports channel subscriptions.
 * <p>
 * Use addSubscriberEventListener() and removeSubscriberEventListener() to start or
 * stop monitoring this class for subscription events. Use the subscribe() and unsubscribe()
 * methods to start or stop listening to specific channels. (Note that you can also
 * specify the channels as an optional parameter list when you invoked connect()).
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public class Subscriber extends EMSClient {

    private static final Logger logger = Logger.getLogger(Subscriber.class.getName());
    protected final List<SubscriberListener> listeners;
    protected SubscriberEvent subscriberEvent;
    SubscriberThread listenerThread;

    /**
     * Creates a new, unconnected Subscriber with no channel subscriptions.
     */
    public Subscriber() {
        listeners = new ArrayList<>();
        subscriberEvent = null;
    }

    @Override
    public boolean connect(String hostAddress, int port) {
        return connect(hostAddress, port, new String[0]);
    }

    @Override
    public EMSServerResponse sendCommand(String command, String... params) {
        // If we're currently listening, stop for a moment:
        boolean wasListening = (listenerThread != null);
        if (wasListening) {
            killListenerThread();
        }

        // Execute the command and note the response:
        EMSServerResponse response = super.sendCommand(command, params);

        // If we were listening before this, start listening again:
        if (wasListening) {
            listenerThread = new SubscriberThread(this, in);
            listenerThread.start();
        }

        return response;
    }

    /**
     * Attempts to connect to the given EMSServer, and subscribes to each of the optional
     * channels listed.
     *
     * @param hostAddress The address or hostname of the server.
     * @param port        The server listening port.
     * @param channels    An optional list of channel names to subscribe to (case sensitive).
     * @return True if the connection was established and all channels were subscribed successfully.
     */
    public boolean connect(String hostAddress, int port, String... channels) {
        boolean success = super.connect(hostAddress, port);
        if (!success) {
            return false;
        }

        // Get our client id:
        EMSServerResponse response = sendCommand("WHO");
        if (!isConnected || response.isError()) {
            logger.log(Level.SEVERE, "Subscriber unable to execute WHO... aborting.");
            disconnect();
            return false;
        }

        // Make sure the server supports SUBSCRIBE and UNSUBSCRIBE commands:
        EMSServerResponse response1 = sendCommand("HELP:SUBSCRIBE");
        EMSServerResponse response2 = sendCommand("HELP:UNSUBSCRIBE");
        if (response1.isError() || response2.isError()) {
            logger.log(Level.SEVERE, "Subscriber: this server does not support SUBSCRIBE/UNSUBSCRIBE.");
            disconnect();
            return false;
        }

        isConnected = true;
        subscriberEvent = new SubscriberEvent(hostAddress, port, response.getMessage());

        for (String channel : channels) {
            subscribe(channel);
        }

        // If any of the subscribe calls failed, we're done.
        if (!isConnected) {
            logger.log(Level.SEVERE, "Subscriber: unable to subscribe to channel list.");
            subscriberEvent = null;
            return false;
        }

        for (SubscriberListener listener : listeners) {
            listener.connected(subscriberEvent);
        }

        // Start a monitoring thread to listen for messages on our channels:
        listenerThread = new SubscriberThread(this, in);
        listenerThread.start();

        return isConnected;
    }

    /**
     * Terminates any active connection. Safe to invoke multiple times - does nothing if unconnected.
     */
    @Override
    public void disconnect() {
        if (isConnected) {
            super.disconnect();
            killListenerThread();
            for (SubscriberListener listener : listeners) {
                listener.disconnected(subscriberEvent);
            }
            subscriberEvent = null;
        }
    }

    void killListenerThread() {
        if (listenerThread != null) {
            listenerThread.shutDown();
            listenerThread.interrupt();
            listenerThread = null;
        }
    }

    public Socket getClientSocket() {
        return this.clientSocket;
    }

    /**
     * Attempts to subscribe to the named channel. In case of failure, the connection is
     * terminated.
     *
     * @param channel The case-sensitive name of the channel to which to subscribe.
     * @return true if all went well. If false, this Subscriber is now disconnected.
     */
    public boolean subscribe(String channel) {
        if (!isConnected) {
            return false;
        }

        EMSServerResponse response = sendCommand("SUB", channel);
        if (!isConnected || response.isError()) {
            logger.log(Level.SEVERE, "Subscriber failed to subscribe to channel... aborting.");
            disconnect();
        }

        return isConnected;
    }

    /**
     * Attempts to unsubscribe from the named channel. In case of failure, the connection is
     * terminated.
     *
     * @param channel The case-sensitive name of the channel from which to unsubscribe.
     * @return true if all went well. If false, this Subscriber is now disconnected.
     */
    public boolean unsubscribe(String channel) {
        if (!isConnected) {
            return false;
        }

        EMSServerResponse response = sendCommand("UNSUB", channel);
        if (!isConnected || response.isError()) {
            logger.log(Level.SEVERE, "Subscriber failed to unsubscribe from channel... aborting.");
            disconnect();
        }

        return isConnected;
    }

    /**
     * Sends a message to the named channel. All subscribers to that channel will be notified
     * with the message content.
     *
     * @param channel The name of the channel in question, or ALL to send to all simultaneously.
     * @param message The message to send. Single line.
     * @return True if all went well. If false, this Subscriber is now disconnected.
     */
    public boolean broadcast(String channel, String message) {
        if (!isConnected) {
            return false;
        }

        EMSServerResponse response = sendCommand("SEND", channel, message);
        if (!isConnected || response.isError()) {
            logger.log(Level.SEVERE, "Subscriber failed to send broadcast message... aborting.");
            disconnect();
        }

        return isConnected;
    }

    void fireMessageReceivedEvent(String channel, String message) {
        subscriberEvent.setChannel(channel);
        for (SubscriberListener listener : listeners) {
            listener.channelMessageReceived(subscriberEvent, message);
        }
    }

    /**
     * The given listener will start receiving notification events from this Subscriber.
     *
     * @param listener The new listener.
     */
    public void addSubscriberEventListener(SubscriberListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * The given listener will no longer receive notification events from this subscriber.
     * Note that this does not unsubscribe this Subscriber from any channels. Messages
     * sent to those channels will still be received by this class, but no longer fowarded
     * on to the given listener.
     *
     * @param listener The listener to be removed.
     */
    public void removeSubscriberEventListener(SubscriberListener listener) {
        listeners.remove(listener);
    }

}
