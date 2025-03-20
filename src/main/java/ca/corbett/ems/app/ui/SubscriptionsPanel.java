package ca.corbett.ems.app.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.audio.AudioUtil;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.CheckBoxField;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.PanelField;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public final class SubscriptionsPanel extends JPanel implements ConnectionListener {

    private static Logger logger = Logger.getLogger(SubscriptionsPanel.class.getName());

    private MessageUtil messageUtil;
    private LabelField statusLabel;
    private LabelField clientIdLabel;
    private JTextArea notificationsArea;
    private CheckBoxField audibleCheckBox;
    private JList<String> availableChannelsList;
    private JList<String> subscribedChannelsList;
    private DefaultListModel<String> availableChannelsListModel;
    private DefaultListModel<String> subscribedChannelsListModel;
    private JButton btnCreate;
    private JButton btnSubscribe;
    private JButton btnRefresh;
    private JButton btnSend;
    private JButton btnUnsubscribe;
    private final int[][] audibleAlert;

    public SubscriptionsPanel() {
        int[][] audio = null;
        try {
            audio = AudioUtil.parseAudioStream(new BufferedInputStream(getClass().getResourceAsStream("/ems-example-app/sfx_bell_ding.wav")));
        } catch (IOException | UnsupportedAudioFileException e) {
            logger.warning("Unable to load audible alert: " + e.getMessage());
        }
        audibleAlert = audio;
        setLayout(new BorderLayout());
        add(buildControlPanel(), BorderLayout.NORTH);
        add(buildAlertsPanel(), BorderLayout.CENTER);
        ConnectionManager.getInstance().addConnectionListener(this);
        enableControls(false);
    }

    public void appendToConsole(String text) {
        notificationsArea.append(text + "\n");
        notificationsArea.setCaretPosition(notificationsArea.getDocument().getLength());
    }

    private JPanel buildControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(buildAvailableChannelsPanel(), BorderLayout.WEST);
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        container.add(buildSubscribedChannelsPanel(), BorderLayout.WEST);
        panel.add(container, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAvailableChannelsPanel() {
        FormPanel formPanel = new FormPanel();

        statusLabel = new LabelField("Server:", "Not connected.");
        formPanel.addFormField(statusLabel);

        PanelField panelField = new PanelField();
        panelField.getPanel().setLayout(new BorderLayout());
        panelField.getPanel().setBorder(BorderFactory.createTitledBorder("Available channels"));
        availableChannelsListModel = new DefaultListModel<>();
        availableChannelsList = new JList<>(availableChannelsListModel);
        JScrollPane scrollPane = new JScrollPane(availableChannelsList);
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        scrollPane.getVerticalScrollBar().setBlockIncrement(64);
        panelField.getPanel().add(scrollPane, BorderLayout.CENTER);
        formPanel.addFormField(panelField);

        panelField = new PanelField();
        panelField.getPanel().setLayout(new FlowLayout(FlowLayout.CENTER));
        btnCreate = new JButton("Create...");
        btnCreate.setPreferredSize(new Dimension(110, 25));
        btnCreate.setFont(btnCreate.getFont().deriveFont(Font.PLAIN, 12f));
        btnCreate.addActionListener(e -> createChannel());
        panelField.getPanel().add(btnCreate);
        btnSubscribe = new JButton("Subscribe");
        btnSubscribe.setPreferredSize(new Dimension(110, 25));
        btnSubscribe.setFont(btnSubscribe.getFont().deriveFont(Font.PLAIN, 12f));
        btnSubscribe.addActionListener(e -> subscribe());
        panelField.getPanel().add(btnSubscribe);
        panelField.setBottomMargin(0);
        formPanel.addFormField(panelField);

        panelField = new PanelField();
        panelField.getPanel().setLayout(new FlowLayout(FlowLayout.CENTER));
        btnRefresh = new JButton("Refresh");
        btnRefresh.setPreferredSize(new Dimension(110, 25));
        btnRefresh.addActionListener(e -> ConnectionManager.getInstance().retrieveChannelList());
        btnRefresh.setFont(btnRefresh.getFont().deriveFont(Font.PLAIN, 12f));
        panelField.getPanel().add(btnRefresh);
        btnSend = new JButton("Send...");
        btnSend.setPreferredSize(new Dimension(110, 25));
        btnSend.setFont(btnSend.getFont().deriveFont(Font.PLAIN, 12f));
        btnSend.addActionListener(e -> sendMessage());
        panelField.getPanel().add(btnSend);
        panelField.setTopMargin(0);
        formPanel.addFormField(panelField);

        formPanel.render();
        return formPanel;
    }

    private JPanel buildSubscribedChannelsPanel() {
        FormPanel formPanel = new FormPanel();

        clientIdLabel = new LabelField("Client Id:", "");
        formPanel.addFormField(clientIdLabel);

        PanelField panelField = new PanelField();
        panelField.getPanel().setLayout(new BorderLayout());
        panelField.getPanel().setBorder(BorderFactory.createTitledBorder("Subscribed channels"));
        subscribedChannelsListModel = new DefaultListModel<>();
        subscribedChannelsList = new JList<>(subscribedChannelsListModel);
        JScrollPane scrollPane = new JScrollPane(subscribedChannelsList);
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        scrollPane.getVerticalScrollBar().setBlockIncrement(64);
        panelField.getPanel().add(scrollPane, BorderLayout.CENTER);
        formPanel.addFormField(panelField);

        panelField = new PanelField();
        panelField.getPanel().setLayout(new FlowLayout(FlowLayout.RIGHT));
        btnUnsubscribe = new JButton("Unsubscribe");
        btnUnsubscribe.setPreferredSize(new Dimension(125, 25));
        btnUnsubscribe.setFont(btnUnsubscribe.getFont().deriveFont(Font.PLAIN, 12f));
        btnUnsubscribe.addActionListener(e -> unsubscribe());
        panelField.getPanel().add(btnUnsubscribe);
        formPanel.addFormField(panelField);

        audibleCheckBox = new CheckBoxField("Audible notification messages", true);
        audibleCheckBox.setEnabled(audibleAlert != null);
        formPanel.addFormField(audibleCheckBox);

        formPanel.render();
        return formPanel;
    }

    private JPanel buildAlertsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Notifications"));

        notificationsArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(notificationsArea);
        notificationsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        notificationsArea.setForeground(Color.GREEN);
        notificationsArea.setBackground(Color.BLACK);
        notificationsArea.setEditable(false);
        notificationsArea.setText("");
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        scrollPane.getVerticalScrollBar().setBlockIncrement(64);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void enableControls(boolean enable) {
        btnCreate.setEnabled(enable);
        btnSubscribe.setEnabled(enable);
        btnSend.setEnabled(enable);
        btnRefresh.setEnabled(enable);
        btnUnsubscribe.setEnabled(enable);
        availableChannelsList.setEnabled(enable);
        subscribedChannelsList.setEnabled(enable);
    }

    private void createChannel() {
        String name = JOptionPane.showInputDialog(MainWindow.getInstance(), "Channel name:");
        if (name != null) {
            if (availableChannelsListModel.contains(name.toUpperCase())) {
                getMessageUtil().info("Channel exists!", "That channel already exists.\nChannel names are case-insensitive.");
                return;
            }
            ConnectionManager.getInstance().subscribe(name);
        }
    }

    private void sendMessage() {
        if (!ConnectionManager.getInstance().isConnected()) {
            return;
        }

        String[] selectedChannels = getSelectedActiveChannels();
        String msg;
        if (selectedChannels.length == 0) {
            msg = "You are broadcasting a message to all channels!\n(You can send to specific channels by selecting them)";
            selectedChannels = new String[]{"ALL"};
        } else if (selectedChannels.length == 1) {
            msg = "Send a message to all subscribers of channel \"" + selectedChannels[0] + "\":";
        } else {
            msg = "You are sending a message to " + selectedChannels.length + " channels:";
        }
        final String toSend = JOptionPane.showInputDialog(MainWindow.getInstance(), msg);
        if (toSend != null) {
            String log = (selectedChannels.length == 1) ? selectedChannels[0] : selectedChannels.length + " channels";
            final String[] channelNames = Arrays.copyOf(selectedChannels, selectedChannels.length);
            appendToConsole("Sending \"" + toSend + "\" to " + log + "...");

            // Invoke later or our appendToConsole above will be blocked until ConnectionManager is done
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ConnectionManager.getInstance().sendMessage(toSend, channelNames);
                    appendToConsole("Send complete!");
                }
            });
        }
    }

    private void subscribe() {
        if (!ConnectionManager.getInstance().isConnected()) {
            return;
        }

        String[] selectedChannels = getSelectedActiveChannels();
        if (selectedChannels.length == 0) {
            getMessageUtil().info("Nothing selected.");
        }
        ConnectionManager.getInstance().subscribe(selectedChannels);
    }

    private String[] getSelectedActiveChannels() {
        int[] selectedArr = availableChannelsList.getSelectedIndices();
        if (selectedArr.length == 0) {
            return new String[]{};
        }
        String[] selectedChannels = new String[selectedArr.length];
        for (int i = 0; i < selectedArr.length; i++) {
            selectedChannels[i] = availableChannelsListModel.get(selectedArr[i]);
        }
        return selectedChannels;
    }

    private void unsubscribe() {
        if (!ConnectionManager.getInstance().isConnected()) {
            return;
        }

        String[] selectedChannels = getSelectedSubscribedChannels();
        if (selectedChannels.length == 0) {
            getMessageUtil().info("Nothing selected.");
        }
        ConnectionManager.getInstance().unsubscribe(selectedChannels);
    }

    private String[] getSelectedSubscribedChannels() {
        int[] selectedArr = subscribedChannelsList.getSelectedIndices();
        if (selectedArr.length == 0) {
            return new String[]{};
        }
        String[] selectedChannels = new String[selectedArr.length];
        for (int i = 0; i < selectedArr.length; i++) {
            selectedChannels[i] = subscribedChannelsListModel.get(selectedArr[i]);
        }
        return selectedChannels;
    }

    @Override
    public void localServerStarted(String host, int port) {

    }

    @Override
    public void localServerStopped() {

    }

    @Override
    public void connected(String host, int port, String serverVersion, String clientId) {
        statusLabel.setText("Connected.");
        clientIdLabel.setText(clientId);
        enableControls(true);
        appendToConsole("Welcome to server \"" + serverVersion + "\"!");
    }

    @Override
    public void disconnected() {
        statusLabel.setText(ServerPanel.DISCONNECTED);
        clientIdLabel.setText("");
        subscribedChannelsListModel.clear();
        availableChannelsListModel.clear();
        enableControls(false);
        appendToConsole("Disconnected.");
    }

    @Override
    public void connectionError(String errorMessage) {
        appendToConsole(errorMessage);
    }

    @Override
    public void channelMessageReceived(String channel, String message) {
        appendToConsole("Message from channel " + channel + ": " + message);
        if (audibleCheckBox.isChecked() && audibleAlert != null) {
            try {
                AudioUtil.play(audibleAlert, null);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void channelList(List<String> activeChannels, List<String> subscribedChannels) {
        availableChannelsListModel.clear();
        subscribedChannelsListModel.clear();
        availableChannelsListModel.addAll(activeChannels);
        subscribedChannelsListModel.addAll(subscribedChannels);
        appendToConsole("Server has " + activeChannels.size() + " active channels, we are subscribed to " + subscribedChannels.size());
    }

    @Override
    public void channelSubscribed(String channelName) {
        appendToConsole("You are now subscribed to channel \"" + channelName + "\".");
    }

    @Override
    public void channelUnsubscribed(String channelName) {
        appendToConsole("You are no longer subscribed to channel \"" + channelName + "\".");
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), logger);
        }
        return messageUtil;
    }
}
