package ca.corbett.ems.app.ui;

import ca.corbett.ems.app.Version;
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
public final class MainWindow extends JFrame implements ConnectionListener {

    private static final Logger logger = Logger.getLogger(MainWindow.class.getName());

    private MessageUtil messageUtil;

    private static MainWindow instance;
    private StatusBar statusBar;

    private MainWindow() {
        super(Version.FULL_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(580, 540));
        setMinimumSize(new Dimension(580, 540));
        initComponents();
        ConnectionManager.getInstance().addConnectionListener(this);
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

    private void initComponents() {
        setLayout(new BorderLayout());

        JTabbedPane tabPane = new JTabbedPane();
        tabPane.add("Intro", buildIntroPanel());
        tabPane.add("Server", new ServerPanel());
        tabPane.add("Subscriptions", new SubscriptionsPanel());
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

        label = new LabelField("<html>The purpose of this app is to show how to extend and customize<br>" +
                "EMSServer and EMSClient and quickly add an API to a Java application.</html>");
        label.setFont(label.getFieldLabelFont().deriveFont(Font.PLAIN, 14));
        label.setExtraMargins(4, 4);
        formPanel.addFormField(label);

        label = new LabelField("<html>In our example, we will start up an EMS server or connect to a<br>" +
                "remote EMS server, and create \"channels\" that clients can subscribe to. <br>" +
                "Then, we can send messages to those channels, and all subscribers will<br>" +
                "receive them. Think of it as an extremely miniature Kafka broker.</html>");
        label.setFont(label.getFieldLabelFont().deriveFont(Font.PLAIN, 14));
        label.setExtraMargins(4, 4);
        formPanel.addFormField(label);

        label = new LabelField("<html>This could be used to create a simple group chat app, or to serve<br>" +
                "as a very basic message broker for Java applications to communicate<br>on a local network.</html>");
        label.setFont(label.getFieldLabelFont().deriveFont(Font.PLAIN, 14));
        label.setExtraMargins(4, 4);
        formPanel.addFormField(label);

        formPanel.render();
        return formPanel;
    }

    private JPanel buildAboutPanel() {
        return new AboutPanel(Version.aboutInfo);
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(instance, logger);
        }
        return messageUtil;
    }

    @Override
    public void localServerStarted(String host, int port) {
    }

    @Override
    public void localServerStopped() {
    }

    @Override
    public void connected(String host, int port, String serverVersion, String clientId) {
        setStatusText("Connected to " + host + ":" + port);
    }

    @Override
    public void disconnected() {
        setStatusText(ServerPanel.DISCONNECTED);
    }

    @Override
    public void connectionError(String errorMessage) {
    }

    @Override
    public void channelMessageReceived(String channel, String message) {
    }

    @Override
    public void channelList(List<String> activeChannels, List<String> subscribedChannels) {
    }

    @Override
    public void channelSubscribed(String channelName) {
    }

    @Override
    public void channelUnsubscribed(String channelName) {
    }
}
