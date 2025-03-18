package ca.corbett.ems.app.subscriber;

/**
 * Can be used to listen for SubscriberEvents on a Subscriber instance.
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public interface SubscriberListener {

    public void connected(SubscriberEvent event);

    public void disconnected(SubscriberEvent event);

    public void channelMessageReceived(SubscriberEvent event, String message);

}
