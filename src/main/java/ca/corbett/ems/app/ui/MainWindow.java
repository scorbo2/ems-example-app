package ca.corbett.ems.app.ui;

import ca.corbett.ems.app.Version;
import ca.corbett.ems.app.handlers.AboutHandler;
import ca.corbett.ems.app.handlers.HaltHandler;
import ca.corbett.ems.app.handlers.HelpHandler;
import ca.corbett.ems.app.handlers.ListActiveHandler;
import ca.corbett.ems.app.handlers.ListSubscribedHandler;
import ca.corbett.ems.app.handlers.SendHandler;
import ca.corbett.ems.app.handlers.SubscribeHandler;
import ca.corbett.ems.app.handlers.UnsubscribeHandler;
import ca.corbett.ems.app.handlers.UptimeHandler;
import ca.corbett.ems.app.subscriber.Subscriber;
import ca.corbett.ems.app.subscriber.SubscriberEvent;
import ca.corbett.ems.app.subscriber.SubscriberListener;
import ca.corbett.ems.client.EMSServerResponse;
import ca.corbett.ems.server.EMSServer;
import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.about.AboutPanel;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.LabelField;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents the main window of the GUI version of the EMS example app.
 * For the headless command line version, refer to Main and CLI instead.
 *
 * @author scorbo2
 * @since 2025-03-18
 */
public final class MainWindow extends JFrame {

    private static final Logger logger = Logger.getLogger(MainWindow.class.getName());

    private static final String DISCONNECTED = "Disconnected.";

    private MessageUtil messageUtil;

    private static MainWindow instance;
    private StatusBar statusBar;
    private ServerPanel serverPanel;

    private EMSServer localServer;
    private Subscriber client;

    private MainWindow() {
        super(Version.FULL_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(500, 400));
        setMinimumSize(new Dimension(500, 400));
        initComponents();
    }

    public static MainWindow getInstance() {
        if (instance == null) {
            instance = new MainWindow();
        }
        return instance;
    }

    public void setStatusText(String text) {
        statusBar.setStatus(text);
    }

    public void startLocalServer(String host, int port) {
        stopLocalServer();
        localServer = new EMSServer(host, port);
        localServer.registerCommandHandler(new AboutHandler());
        localServer.registerCommandHandler(new HelpHandler());
        localServer.registerCommandHandler(new SendHandler());
        localServer.registerCommandHandler(new ListActiveHandler());
        localServer.registerCommandHandler(new ListSubscribedHandler());
        localServer.registerCommandHandler(new SubscribeHandler());
        localServer.registerCommandHandler(new UnsubscribeHandler());
        localServer.registerCommandHandler(new HaltHandler());
        localServer.registerCommandHandler(new UptimeHandler());
        localServer.startServer(); // we could spy on it for extra logging, but it'll get noisy
        serverPanel.appendToConsole("Started local EMS server on port " + port);
    }

    public void stopLocalServer() {
        if (isLocalServerRunning()) {
            localServer.stopServer();
        }

        localServer = null;
    }

    public void connect(String host, int port) {
        if (client != null && client.isConnected()) {
            client.disconnect();
            serverPanel.appendToConsole("Disconnected.");
        }
        client = new Subscriber();
        client.addSubscriberEventListener(new SubscriberListener() {
            @Override
            public void connected(SubscriberEvent event) {
                // ignored
            }

            @Override
            public void disconnected(SubscriberEvent event) {
                MainWindow.getInstance().disconnect();
            }

            @Override
            public void channelMessageReceived(SubscriberEvent event, String message) {
                // TODO do something with this
            }
        });
        if (client.connect(host, port)) {
            String msg = "Connected to " + host + ":" + port;
            setStatusText(msg);
            serverPanel.appendToConsole(msg);
            executeStartupCommands();
        } else {
            getMessageUtil().error("Unable to connect.");
            serverPanel.appendToConsole("Unable to connect.");
            setStatusText(DISCONNECTED);
        }
    }

    public void disconnect() {
        if (isConnected()) {
            client.disconnect();
            serverPanel.appendToConsole("Disconnected.");
        }
        if (isLocalServerRunning()) {
            stopLocalServer();
            serverPanel.appendToConsole("Local server shut down.");
        }
        serverPanel.appendToConsole("\n");

        client = null;
        setStatusText(DISCONNECTED);
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public boolean isLocalServerRunning() {
        return localServer != null && localServer.isUp();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        serverPanel = new ServerPanel();

        JTabbedPane tabPane = new JTabbedPane();
        tabPane.add("Intro", buildIntroPanel());
        tabPane.add("Server", serverPanel);
        tabPane.add("Subscriptions", buildSubscriptionsPanel());
        tabPane.add("About", buildAboutPanel());
        add(tabPane, BorderLayout.CENTER);

        statusBar = new StatusBar();
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel buildIntroPanel() {
        FormPanel formPanel = new FormPanel();

        LabelField label = new LabelField(Version.FULL_NAME);
        label.setFont(label.getFieldLabelFont().deriveFont(Font.BOLD, 18f));
        label.setExtraMargins(16, 0);
        formPanel.addFormField(label);

        label = new LabelField("<html>The purpose of this app is to show how to extend<br>" +
                "and customize EMSServer and EMSClient and quickly add<br>" +
                "an API to a Java application.</html>");
        label.setFont(label.getFieldLabelFont().deriveFont(Font.PLAIN, 14));
        label.setExtraMargins(4, 4);
        formPanel.addFormField(label);

        label = new LabelField("<html>In our example, we will start up an EMS server<br>" +
                "or connect to a remote EMS server, and create \"channels\" that<br>" +
                "clients can subscribe to. Then, we can send messages<br>" +
                "to those channels, and all subscribers will receive them.<br>" +
                "Think of it as an extremely miniature Kafka broker.</html>");
        label.setFont(label.getFieldLabelFont().deriveFont(Font.PLAIN, 14));
        label.setExtraMargins(4, 4);
        formPanel.addFormField(label);

        label = new LabelField("<html>This could be used to create a simple group chat app,<br>" +
                "or to serve as a very basic message broker for Java<br>" +
                "applications to communicate on a local network.</html>");
        label.setFont(label.getFieldLabelFont().deriveFont(Font.PLAIN, 14));
        label.setExtraMargins(4, 4);
        formPanel.addFormField(label);

        formPanel.render();
        return formPanel;
    }

    private JPanel buildSubscriptionsPanel() {
        JPanel panel = new JPanel();
        return panel;
    }

    private JPanel buildAboutPanel() {
        return new AboutPanel(Version.aboutInfo);
    }

    /**
     * When connecting to an EMS server, local or remote, we'll execute a
     * couple of test commands to verify the connection, and output the
     * results to the server console.
     */
    private void executeStartupCommands() {
        if (!isConnected()) {
            return;
        }

        // Get server version:
        EMSServerResponse response = client.sendCommand("about");
        if (response.isSuccess()) {
            serverPanel.appendToConsole("Server version is: " + response.getMessage());
        } else {
            serverPanel.appendToConsole("Server version request returned error.");
            getMessageUtil().error("Unable to query server version! Disconnecting.");
            disconnect();
        }

        // Output our client identifier:
        response = client.sendCommand("who");
        if (response.isSuccess()) {
            serverPanel.appendToConsole("Connected as client " + response.getMessage());
        } else {
            String msg = "Unable to retrieve client id. Disconnecting.";
            serverPanel.appendToConsole(msg);
            getMessageUtil().error(msg);
        }

        // Get the list of active channels:
        serverPanel.appendToConsole("Retrieving channel list from server...");
        List<String> channels = client.getActiveChannels();
        serverPanel.appendToConsole("Found " + channels.size() + " channels on this server.");
        // TODO do something with channels... populate subscriptions panel
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(instance, logger);
        }
        return messageUtil;
    }
}
