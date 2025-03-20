package ca.corbett.ems.app.ui;

import ca.corbett.ems.app.Version;
import ca.corbett.ems.app.handlers.AboutHandler;
import ca.corbett.extras.MessageUtil;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.NumberField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.forms.fields.TextField;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A JPanel that contains controls for starting a local EMS server or for
 * connecting to a remote one. Also contains a server output console
 * for visibility as to what's going on within the server.
 *
 * @author scorbo2
 * @since 2025-03-18
 */
public final class ServerPanel extends JPanel implements ConnectionListener {

    private static final Logger logger = Logger.getLogger(ServerPanel.class.getName());

    public static final String DISCONNECTED = "Not connected.";

    private MessageUtil messageUtil;
    private ComboField sourceField;
    private TextField nameField;
    private TextField hostField;
    private NumberField portField;
    private JTextArea textArea;

    public ServerPanel() {
        setLayout(new BorderLayout());
        add(buildControlPanel(), BorderLayout.NORTH);
        add(buildConsole(), BorderLayout.CENTER);
        ConnectionManager.getInstance().addConnectionListener(this);
    }

    public void clearConsole() {
        textArea.setText("");
    }

    public void appendToConsole(String text) {
        textArea.append(text + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    private JPanel buildControlPanel() {
        FormPanel formPanel = new FormPanel(FormPanel.Alignment.TOP_LEFT);

        List<String> options = new ArrayList<>();
        options.add("Start a local EMS server");
        options.add("Connect to a remote EMS server");
        sourceField = new ComboField("Source:", options, 0, false);
        sourceField.addValueChangedAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nameField.setEnabled(sourceField.getSelectedIndex() == 0);
            }
        });
        formPanel.addFormField(sourceField);

        nameField = new TextField("Name:", 15, 1, true);
        nameField.setText(Version.FULL_NAME);
        formPanel.addFormField(nameField);

        hostField = new TextField("Host:", 15, 1, true);
        hostField.setText("localhost");
        formPanel.addFormField(hostField);

        portField = new NumberField("Port:", 1975, 1024, 65535, 1);
        formPanel.addFormField(portField);

        PanelField panelField = new PanelField();
        panelField.getPanel().setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton btn = new JButton("Connect");
        btn.setPreferredSize(new Dimension(120, 25));
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (sourceField.getSelectedIndex() == 0) {
                    appendToConsole("Starting local EMS server...");
                    AboutHandler.getInstance().setServerName(nameField.getText());
                    ConnectionManager.getInstance().startLocalServer(hostField.getText(), (Integer) portField.getCurrentValue());
                }
                appendToConsole("Attempting to connect...");
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(75); // give it a chance to start up
                        } catch (InterruptedException ignored) {
                        }
                        ConnectionManager.getInstance().connect(hostField.getText(), (Integer) portField.getCurrentValue());
                    }
                });
            }
        });
        panelField.getPanel().add(btn);
        btn = new JButton("Disconnect");
        btn.setPreferredSize(new Dimension(120, 25));
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ConnectionManager.getInstance().disconnect();
            }
        });
        panelField.getPanel().add(btn);
        formPanel.addFormField(panelField);

        formPanel.render();
        return formPanel;
    }

    private JPanel buildConsole() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setForeground(Color.GREEN);
        textArea.setBackground(Color.BLACK);
        textArea.setEditable(false);
        textArea.setRows(8);
        textArea.setText(Version.FULL_NAME + "\n" + Version.PROJECT_URL + "\n" + "Ready.\n");
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        scrollPane.getVerticalScrollBar().setBlockIncrement(64);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), logger);
        }
        return messageUtil;
    }

    @Override
    public void localServerStarted(String host, int port) {
        appendToConsole("Started local EMS server on port " + port);
    }

    @Override
    public void localServerStopped() {
        appendToConsole("Local server shut down.\n");
    }

    @Override
    public void connected(String host, int port, String serverVersion, String clientId) {
        appendToConsole("Connected to " + host + ":" + port);
        appendToConsole("Server version is: " + serverVersion);
        appendToConsole("Connected as client " + clientId);
    }

    @Override
    public void disconnected() {
        appendToConsole("Disconnected.");
    }

    @Override
    public void connectionError(String errorMessage) {
        appendToConsole(errorMessage);
        getMessageUtil().error(errorMessage);
    }

    @Override
    public void channelMessageReceived(String channel, String message) {

    }

    @Override
    public void channelList(List<String> activeChannels, List<String> subscribedChannels) {
        appendToConsole("Found " + activeChannels.size() + " channels on this server.");
    }

    @Override
    public void channelSubscribed(String channelName) {
    }

    @Override
    public void channelUnsubscribed(String channelName) {
    }
}
