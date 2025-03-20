package ca.corbett.ems.app.ui;

import java.util.List;

/**
 * Provides a way for the UI classes to listen to ConnectionManager for
 * various events, so that they can respond to those events by updating the UI.
 * The logic for managing the EMS server connection can be managed
 * entirely within ConnectionManager, without ConnectionManager having
 * to know anything at all about the UI code. It's a clean separation.
 *
 * @author scorbo2
 * @since 2025-03-19
 */
public interface ConnectionListener {

    /**
     * Fired when a local EMS server has been successfully started.
     * The "host" parameter will usually be "localhost", but not necessarily...
     * machines with multiple network interfaces could pick a specific IP address
     * to bind to.
     *
     * @param host The host or IP address of the local server.
     * @param port The port on which the new EMS server is listening.
     */
    public void localServerStarted(String host, int port);

    /**
     * Fired when a local EMS server has stopped.
     */
    public void localServerStopped();

    /**
     * Fired when ConnectionManager has successfully connected to either a local EMS
     * server or to a remote one.
     *
     * @param host          The host to which we are connected.
     * @param port          The listening port of the EMS server.
     * @param serverVersion The version string that the EMS server reported via the ABOUT command.
     * @param clientId      The client id that the EMS server assigned us.
     */
    public void connected(String host, int port, String serverVersion, String clientId);

    /**
     * Invoked when ConnectionManager has disconnected from either a local EMS server
     * or from a remote one.
     */
    public void disconnected();

    /**
     * Invoked when an error occurs while communicating with an EMS server.
     *
     * @param errorMessage A hopefully enlightening error message.
     */
    public void connectionError(String errorMessage);

    /**
     * Invoked when a message arrives on any of the channels that ConnectionManager
     * is currently subscribed to.
     *
     * @param channel The channel that delivered the message.
     * @param message The message.
     */
    public void channelMessageReceived(String channel, String message);

    /**
     * Invoked whenever the list of active and subscribed channels has been successfully
     * retrieved from the EMS server.
     *
     * @param activeChannels A list of all active channels on the EMS server.
     * @param subscribedChannels A list of all channels that ConnectionManager is currently subscribed to.
     */
    public void channelList(List<String> activeChannels, List<String> subscribedChannels);

    /**
     * Invoked when ConnectionManager has successfully subscribed to a channel.
     *
     * @param channelName The name of the channel to which we are now subscribed.
     */
    public void channelSubscribed(String channelName);

    /**
     * Invoked when ConnectionManager has successfully unsubscribed from a channel.
     *
     * @param channelName The name of the channel from which we are now unsubscribed.
     */
    public void channelUnsubscribed(String channelName);
}
