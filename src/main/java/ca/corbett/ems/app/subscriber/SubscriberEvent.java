package ca.corbett.ems.app.subscriber;

/**
 * Fired by Subscriber when something happens on one of the subscribed channels.
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public class SubscriberEvent {

    protected final String remoteHostAddress;
    protected final int remotePort;
    protected final String clientId;
    protected String channel;

    /**
     * Created as needed by Subscriber - you generally should never need to manually
     * instantiate this class.
     *
     * @param address  The address of the EMSServer in question.
     * @param port     The listening port of the server.
     * @param clientId The unique id of the client in question.
     */
    public SubscriberEvent(String address, int port, String clientId) {
        this.remoteHostAddress = address;
        this.remotePort = port;
        this.clientId = clientId;
        channel = "";
    }

    public String getRemoteHostAddress() {
        return remoteHostAddress;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public String getClientId() {
        return clientId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

}
