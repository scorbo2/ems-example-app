package ca.corbett.ems.app;

import ca.corbett.ems.app.handlers.HaltHandler;
import ca.corbett.ems.app.handlers.UptimeHandler;
import ca.corbett.ems.app.ui.MainWindow;
import ca.corbett.ems.client.EMSClient;
import ca.corbett.ems.client.EMSServerResponse;
import ca.corbett.ems.client.channel.Subscriber;
import ca.corbett.ems.client.channel.SubscriberEvent;
import ca.corbett.ems.client.channel.SubscriberListener;
import ca.corbett.ems.handlers.VersionHandler;
import ca.corbett.ems.server.EMSServer;
import ca.corbett.ems.server.EMSServerSpy;
import org.apache.commons.cli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * A standalone example implementation of the EMSServer. This implementation
 * supplies some custom command handlers to implement a very lightweight kafka-like message
 * channel pub/sub model, where clients can subscribe to specific channels and receive messages
 * on them, and also send messages to specific channels.
 * <p>
 *     The application can run entirely headless or can present a GUI depending on
 *     command line arguments. Refer to the CLI class for a full description or
 *     use -h on the command line for help.
 * </p>
 * <p>
 *     Logging is configurable! You can specify your own logging.properties file if you
 *     don't like the default log format and options. This file can either be placed in
 *     the directory from which you launch EMS or it can be placed anywhere you like
 *     and specified with -Djava.util.logging.config.file=/some/path/to/logging.properties
 * </p>
 * <p>
 *     By default, all log output goes to the console.
 * </p>
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public class Main {

    private static Logger logger;

    public static void main(String[] args) {
        configureLogging();
        CommandLine cmdLine = CLI.generateCommandLine(args);

        // Show help or version if needed:
        if (cmdLine.hasOption("help")) {
            CLI.showUsageText(true);
        }
        if (cmdLine.hasOption("version")) {
            System.out.println(Version.FULL_NAME);
            return;
        }

        // Otherwise, we need one of startServer, startClient, startSubscriber, or startGui:
        boolean startServer = cmdLine.hasOption("startServer");
        boolean startClient = cmdLine.hasOption("startClient");
        boolean startSubscriber = cmdLine.hasOption("startSubscriber");
        boolean startGui = cmdLine.hasOption("startGui");

        // We can't do more than one of those, though:
        int commandCount = 0;
        commandCount += startServer ? 1 : 0;
        commandCount += startClient ? 1 : 0;
        commandCount += startSubscriber ? 1 : 0;
        commandCount += startGui ? 1 : 0;
        if (commandCount != 1) {
            System.err.println("Error: Exactly one of startServer, startClient, startSubscriber, or startGui is required.");
            CLI.showUsageText(true);
        }

        // Start up the GUI if requested:
        if (startGui) {
            MainWindow.getInstance().setVisible(true);
            return;
        }

        // Collect extra options as needed:
        String channel = cmdLine.getOptionValue("channel");
        boolean serverSpy = cmdLine.hasOption("serverSpy");
        if (startSubscriber) {
            if (channel == null || channel.isBlank()) {
                System.err.println("Error: --startSubscriber requires --channel");
                CLI.showUsageText(true);
            }
        }

        // EMS servers can optionally be assigned a human-readable name:
        if (cmdLine.hasOption("serverName")) {
            VersionHandler.getInstance().setServerName(cmdLine.getOptionValue("serverName"));
        }

        // Host and port if specified:
        String host = cmdLine.hasOption("host") ? cmdLine.getOptionValue("host") : CLI.DEFAULT_HOSTNAME;
        int port = CLI.DEFAULT_LISTENING_PORT;
        if (cmdLine.hasOption("port")) {
            String portStr = cmdLine.getOptionValue("port");
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException nfe) {
                System.err.println("Error: Invalid port value \"" + portStr + "\"");
                return;
            }
        }

        // Now that all the startup stuff is done, we can start putting our output to
        // a Logger instead of to stdout.
        logger = Logger.getLogger(Main.class.getName());
        logger.info(Version.FULL_NAME + " starting up...");

        // Okay, do it:
        if (startServer) {
            startServer(host, port, serverSpy);
        }
        else if (startClient) {
            startClient(host, port);
        }
        else if (startSubscriber) {
            startSubscriber(host, port, channel);
        }
    }

    /**
     * Spins up an EMS server on the given host and port and lets it run until interrupted.
     * (Use ctrl+c on the command line to kill it).
     *
     * @param host      The hostname or IP to bind to (typically just "localhost").
     * @param port      The port to listen on (must be available).
     * @param serverSpy Whether to add a server spy for more log output (gets noisy).
     */
    private static void startServer(String host, int port, boolean serverSpy) {
        logger.info("Starting up an EMS server on " + host + ":" + port);

        // Register all our command handlers:
        EMSServer server = new EMSServer(host, port);
        server.registerCommandHandler(new HaltHandler());
        server.registerCommandHandler(new UptimeHandler());

        // Add a logging server spy if requested.
        // This just output log info every time the server sends or receives anything.
        // This is handy for debugging but gets real noisy real quick on an actual server.
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

                @Override
                public void clientConnected(EMSServer server, String clientId) {
                }

                @Override
                public void clientDisconnected(EMSServer server, String clientId) {
                }
            });
        }

        // Start the server:
        server.startServer();
    }

    /**
     * Starts an interactive EMS client and accepts keyboard input to drive it.
     * This requires knowledge as to the exact command structure that EMS server expects.
     * If you get stuck, try "help".
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     */
    public static void startClient(String host, int port) {
        logger.info("Starting up an EMS client connecting to \"" + host + ":" + port + "\"...");
        EMSClient client = new EMSClient();
        if (!client.connect(host, port)) {
            logger.severe("Error: unable to connect.");
            return;
        }
        logger.info("Connected. Type \"quit\" to disconnect or \"?\" for help.");
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
                        logger.info("Client disconnected.");
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
                        if (!msg.endsWith("\n")) {
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
            logger.severe("Error: caught exception: " + ioe.getMessage());
            client.disconnect();
        }
    }

    /**
     * Starts an EMS subscriber and subscribes to the given channel on the given host and port.
     * Any message sent to that channel will be output to the console.
     */
    public static void startSubscriber(String host, int port, String channel) {
        logger.info("Starting up an EMS subscriber connecting to \"" + host + ":" + port + "\"...");

        Subscriber subscriber = new Subscriber();
        if (!subscriber.connect(host, port)) {
            logger.severe("Error: unable to connect.");
            return;
        }
        logger.info("Subscribing to channel \"" + channel + "\"... ");
        if (!subscriber.subscribe(channel)) {
            logger.severe("Error: unable to subscribe.");
            return;
        }
        logger.info("You are now subscribed to channel: " + channel);
        subscriber.addSubscriberEventListener(new SubscriberListener() {
            @Override
            public void connected(SubscriberEvent event) {
            }

            @Override
            public void disconnected(SubscriberEvent event) {
                logger.info("Disconnected - terminating.");
                System.exit(0);
            }

            @Override
            public void channelMessageReceived(SubscriberEvent event, String message) {
                logger.info(message);
            }

        });
        logger.info("Listening for messages. Type \"quit\" to stop listening.");
        logger.info("Anything else typed here will be broadcast to that channel.");
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
                        logger.severe("Error: unable to send. Disconnecting.");
                        subscriber.disconnect();
                        return;
                    }
                }
            } while (command != null && !command.equalsIgnoreCase("QUIT") && subscriber.isConnected());
        } catch (IOException ioe) {
            logger.severe("Error: caught exception: " + ioe.getMessage());
            subscriber.disconnect();
        }
    }

    /**
     * Logging can use the EMS built-in configuration, or you can supply your own logging properties file.
     * <ol>
     *     <li><b>Built-in logging.properties</b>: the jar file comes packaged with a default logging.properties
     *     file that you can use. You don't need to do anything to activate this config: this is the default.</li>
     *     <li><b>Specify your own</b>: you can create a logging.properties file and put it in the directory
     *     from which you launch EMS. It will be detected and used. OR you can start EMS with the
     *     -Djava.util.logging.config.file= option, in which case you can point it to wherever your
     *     logging.properties file lives.</li>
     * </ol>
     */
    private static void configureLogging() {
        // If the java.util.logging.config.file System property exists, do nothing.
        // It will be used automatically.
        if (System.getProperties().containsKey("java.util.logging.config.file")) {
            return;
        }

        // Otherwise, see if we can spot a logging.properties file in the current dir:
        File propsFile = new File("logging.properties");
        if (propsFile.exists() && propsFile.canRead()) {
            System.setProperty("java.util.logging.config.file", propsFile.getAbsolutePath());
            return;
        }

        // Otherwise, load the built-in config:
        try {
            LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/ems-example-app/logging.properties"));
        } catch (IOException ioe) {
            System.out.println("WARN: Unable to load log configuration: " + ioe.getMessage());
        }
    }
}
