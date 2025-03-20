package ca.corbett.ems.app.ui;

import java.util.List;

/**
 * Provides a way for the UI classes to listen to ConnectionManager for
 * various events, so that they can respond to those events by updating the UI.
 *
 * @author scorbo2
 * @since 2025-03-19
 */
public interface ConnectionListener {

    public void localServerStarted(String host, int port);

    public void localServerStopped();

    public void connected(String host, int port, String serverVersion, String clientId);

    public void disconnected();

    public void connectionError(String errorMessage);

    public void channelMessageReceived(String channel, String message);

    public void channelList(List<String> activeChannels, List<String> subscribedChannels);

    public void channelSubscribed(String channelName);

    public void channelUnsubscribed(String channelName);
}
