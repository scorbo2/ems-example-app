package ca.corbett.ems.app;

import ca.corbett.ems.app.handlers.AboutHandler;
import ca.corbett.ems.app.handlers.HaltHandler;
import ca.corbett.ems.app.handlers.HelpHandler;
import ca.corbett.ems.app.handlers.SendHandler;
import ca.corbett.ems.app.handlers.SubscribeHandler;
import ca.corbett.ems.app.handlers.UnsubscribeHandler;
import ca.corbett.ems.app.handlers.UptimeHandler;
import ca.corbett.ems.app.subscriber.Subscriber;
import ca.corbett.ems.app.subscriber.SubscriberEvent;
import ca.corbett.ems.app.subscriber.SubscriberListener;
import ca.corbett.ems.client.EMSClient;
import ca.corbett.ems.client.EMSServerResponse;
import ca.corbett.ems.server.EMSServer;
import ca.corbett.ems.server.EMSServerSpy;
import org.apache.commons.cli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A standalone example implementation of the EMSServer. This implementation
 * supplies some custom command handlers to implement a very lightweight kafka-like message
 * channel pub/sub model, where clients can subscribe to specific channels and receive messages
 * on them, and also send messages to specific channels.
 * <p>
 *     You can start the application using command line arguments for a console-style
 *     interface (i.e. in a headless environment), or you can supply no command line
 *     arguments to launch the UI. Refer to the CLI class for command line arguments,
 *     or pass -h on the command line for help.
 * </p>
 *
 * @author scorbo2
 * @since 2024-11-24
 */
public class Main {

    public static void main(String[] args) {
        CommandLine commandLine = CLI.generateCommandLine(args);
        String host = CLI.DEFAULT_HOSTNAME;
        int port = CLI.DEFAULT_LISTENING_PORT;
        String channel = "";
        boolean serverSpy = false;

        // Show help or version if needed:
        if (commandLine.hasOption("help")) {
            CLI.showUsageText(true);
        }
        if (commandLine.hasOption("version")) {
            System.out.println(Version.FULL_NAME);
            return;
        }

        // If none of the "start" arguments were given, launch the GUI:
        boolean startServer = commandLine.hasOption("startServer");
        boolean startClient = commandLine.hasOption("startClient");
        boolean startSubscriber = commandLine.hasOption("startSubscriber");
        if (!startServer && !startClient && !startSubscriber) {
            CLI.showUsageText(true);
            // TODO launch gui here
        }

        // We can't do more than one of those, though:
        int howManyThingsDoYouWantMeToDo = 0;
        if (startServer) {
            howManyThingsDoYouWantMeToDo++;
            serverSpy = commandLine.hasOption("serverSpy");
        }
        if (startClient) {
            howManyThingsDoYouWantMeToDo++;
        }
        if (startSubscriber) {
            howManyThingsDoYouWantMeToDo++;
            if (!commandLine.hasOption("channel")) {
                System.out.println("Error: --startSubscriber requires --channel");
                CLI.showUsageText(true);
            }
            channel = commandLine.getOptionValue("channel");
        }
        if (howManyThingsDoYouWantMeToDo != 1) {
            System.out.println("Error: Too many options specified.");
            CLI.showUsageText(true);
        }

        // Gather other options:
        if (commandLine.hasOption("host")) {
            host = commandLine.getOptionValue("host");
        }
        if (commandLine.hasOption("port")) {
            String portStr = commandLine.getOptionValue("port");
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException nfe) {
                System.out.println("Error: Invalid port value \"" + portStr + "\"");
                return;
            }
        }

        final Logger logger = Logger.getLogger(Main.class.getName());
        logger.info(Version.FULL_NAME + " starting up...");

        // Starting a server?
        if (startServer) {
            System.out.println("Starting up an EMS server on "+host+":"+port);
            // Register all our command handlers:
            EMSServer server = new EMSServer(host, port);
            server.registerCommandHandler(new AboutHandler());
            server.registerCommandHandler(new HelpHandler());
            server.registerCommandHandler(new SendHandler());
            server.registerCommandHandler(new SubscribeHandler());
            server.registerCommandHandler(new UnsubscribeHandler());
            server.registerCommandHandler(new HaltHandler());
            server.registerCommandHandler(new UptimeHandler());

            if (serverSpy) {
                server.addServerSpy(new EMSServerSpy() {
                    @Override
                    public void messageReceived(EMSServer server, String clientId, String rawMessage) {
                        logger.log(Level.INFO, "Spy: {0} sent \"{1}\"", new Object[]{clientId, rawMessage});
                    }

                    @Override
                    public void messageSent(EMSServer server, String clientId, String rawMessage) {
                        logger.log(Level.INFO, "Spy: sending \"{0}\" to {1}", new Object[]{rawMessage, clientId});
                    }

                });
            }

            // Start the server:
            server.startServer();
        }

        // Starting a client?
        else if (startClient) {
            System.out.print("Starting up an EMS client connecting to \"" + host + ":" + port + "\"...");
            EMSClient client = new EMSClient();
            if (!client.connect(host, port)) {
                System.out.println();
                System.out.println("Error: unable to connect.");
                return;
            }
            System.out.println();
            System.out.println("Connected. Type \"quit\" to disconnect or \"?\" for help.");
            System.out.print(">");
            try {
                // Extremely basic command line parser follows!
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String command;
                do {
                    command = reader.readLine();
                    if (command != null && !command.trim().isEmpty()) {
                        command = command.trim();
                        if (command.equalsIgnoreCase("QUIT") || !client.isConnected()) {
                            System.out.println("Client disconnected.");
                            client.disconnect();
                            return;
                        }
                        String[] params = new String[0];
                        if (command.contains(EMSServer.DELIMITER)) {
                            String[] parts = command.split(EMSServer.DELIMITER);
                            command = parts[0];
                            params = new String[parts.length - 1];
                            for (int i = 1; i < parts.length; i++) {
                                params[i - 1] = parts[i];
                            }
                        }
                        EMSServerResponse response = client.sendCommand(command, params);
                        String msg = response.getMessage();
                        if (!msg.isBlank()) {
                            System.out.print(msg);
                            if (! msg.endsWith("\n")) {
                                System.out.println();
                            }
                        }
                        System.out.println(response.isError() ? EMSServer.RESPONSE_ERR : "");
                        System.out.print(">");
                        if (response.isServerDisconnectError()) {
                            client.disconnect();
                            break;
                        }
                    }
                } while (command != null && !command.equalsIgnoreCase("QUIT"));
            } catch (IOException ioe) {
                System.out.println("Error: caught exception: " + ioe.getMessage());
                client.disconnect();
            }
        }

        // Starting a subscriber?
        else if (startSubscriber) {
            System.out.print("Starting up an EMS subscriber connecting to \"" + host + ":" + port + "\"...");
            Subscriber subscriber = new Subscriber();
            if (!subscriber.connect(host, port)) {
                System.out.println();
                System.out.println("Error: unable to connect.");
                return;
            }
            System.out.println();
            System.out.print("Subscribing to channel \"" + channel + "\"... ");
            if (!subscriber.subscribe(channel)) {
                System.out.println();
                System.out.println("Error: unable to subscribe.");
                return;
            }
            System.out.println("subscribed!");
            subscriber.addSubscriberEventListener(new SubscriberListener() {
                @Override
                public void connected(SubscriberEvent event) {
                }

                @Override
                public void disconnected(SubscriberEvent event) {
                    System.out.println("Disconnected - terminating.");
                    System.exit(0);
                }

                @Override
                public void channelMessageReceived(SubscriberEvent event, String message) {
                    System.out.println(message);
                }

            });
            System.out.println("Listening for messages. Type \"quit\" to stop listening.");
            System.out.println("Anything else typed here will be broadcast to that channel.");
            try {
                // Extremely basic command line parser follows!
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String command;
                do {
                    command = reader.readLine();
                    if (command != null && !command.trim().isEmpty()) {
                        command = command.trim();
                        if (command.equalsIgnoreCase("QUIT")) {
                            subscriber.disconnect();
                            return;
                        }
                        if (!subscriber.broadcast(channel, command)) {
                            System.out.println("Error: unable to send. Disconnecting.");
                            subscriber.disconnect();
                            return;
                        }
                    }
                } while (command != null && !command.equalsIgnoreCase("QUIT") && subscriber.isConnected());
            } catch (IOException ioe) {
                System.out.println("Error: caught exception: " + ioe.getMessage());
                subscriber.disconnect();
            }
        }
    }
}
