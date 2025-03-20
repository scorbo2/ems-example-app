package ca.corbett.ems.app.subscriber;

/**
 * Can be used to listen for SubscriberEvents on a Subscriber instance.
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public interface SubscriberListener {

    /**
     * Fired when the Subscriber has successfully connected to an EMS server.
     *
     * @param event The SubscriberEvent for this event.
     */
    public void connected(SubscriberEvent event);

    /**
     * Fired when the Subscriber has disconnected from an EMS server.
     *
     * @param event The SubscriberEvent for this event.
     */
    public void disconnected(SubscriberEvent event);

    /**
     * Fired when a message arrives on one of the channels to which this
     * Subscriber is subscribed. The event parameter will contain the
     * specific channel name.
     *
     * @param event   The SubscriberEvent for this event.
     * @param message The message that arrived.
     */
    public void channelMessageReceived(SubscriberEvent event, String message);

}
