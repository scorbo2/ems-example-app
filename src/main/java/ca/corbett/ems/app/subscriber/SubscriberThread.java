package ca.corbett.ems.app.subscriber;

import ca.corbett.ems.server.EMSServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An internal class used by Subscriber to listen for messages from the EMSServer.
 *
 * @author scorbo2
 * @since 2023-11-24
 */
class SubscriberThread extends Thread {

    private static final Logger logger = Logger.getLogger(SubscriberThread.class.getName());

    private final Subscriber owner;
    private final BufferedReader in;
    private volatile boolean isRunning;

    /**
     * Created as needed by Subscriber to manage a connection to an EMS server. You should
     * generally never need to instantiate this.
     *
     * @param owner The Subscriber that owns this thread.
     * @param in    The BufferedReader from which to read our input.
     */
    public SubscriberThread(Subscriber owner, BufferedReader in) {
        this.owner = owner;
        this.in = in;
    }

    @Override
    public void run() {
        isRunning = true;
        try {
            while (!isInterrupted() && isRunning) {
                if (in == null || owner.getClientSocket() == null || owner.getClientSocket().isClosed()) {
                    break;
                }
                if (!in.ready()) {
                    Thread.sleep(500);
                    continue;
                }
                if (!isRunning || isInterrupted()) {
                    break;
                }

                String inputLine = in.readLine();

                // Check for server disconnections (these are bad, and require us to notify our owner):
                if (inputLine == null || EMSServer.DISCONNECTED.equals(inputLine)) {
                    logger.log(Level.SEVERE, "Received disconnect from server.");
                    owner.disconnect();
                    break;
                }

                // Check for interrupts (these are no big deal and were probably triggered by our owner):
                if (!isRunning || isInterrupted()) {
                    break;
                }

                // The raw message will be in the form "channel:message"
                // If there's no delimiter, something has gone wrong.
                String rawMessage = inputLine.trim();
                if (!rawMessage.contains(EMSServer.DELIMITER)) {
                    logger.log(Level.SEVERE, "Subscriber received unexpected message from server: \"{0}\"", rawMessage);
                    owner.disconnect();
                    break;
                }

                // Wonky case: the message might be empty (i.e. rawMessage is "channel:")
                int delimiterIndex = rawMessage.indexOf(EMSServer.DELIMITER);
                String channel = rawMessage.substring(0, delimiterIndex);
                String message = "";
                if (delimiterIndex < (rawMessage.length() - 1)) {
                    message = rawMessage.substring(delimiterIndex + 1);
                }

                owner.fireMessageReceivedEvent(channel, message);
            }
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Caught exception in subscriber loop", ioe);
        } catch (InterruptedException ignored) {
            // This is normal when sending to own channel - thread gets killed then restarted to avoid sending to self
        }

        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void shutDown() {
        isRunning = false;
    }

    @Override
    public void interrupt() {
        isRunning = false;
        super.interrupt();
    }

}
